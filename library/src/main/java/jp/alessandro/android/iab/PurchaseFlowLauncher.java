/*
 *  Copyright (C) 2016 Alessandro Yuichi Okimoto
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *  Contact email: alessandro@alessandro.jp
 */

package jp.alessandro.android.iab;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;

import java.util.List;
import java.util.Locale;

import jp.alessandro.android.iab.logger.Logger;

public class PurchaseFlowLauncher {

    private final String mItemType;
    private final String mPublicKeyBase64;
    private final int mApiVersion;
    private final String mPackageName;
    private final Logger mLogger;
    private int mRequestCode;

    PurchaseFlowLauncher(BillingContext context, String itemType) {
        mItemType = itemType;
        mPublicKeyBase64 = context.getPublicKeyBase64();
        mApiVersion = context.getApiVersion();
        mPackageName = context.getContext().getPackageName();
        mLogger = context.getLogger();
    }

    public void launch(IInAppBillingService service,
                       Activity activity,
                       int requestCode,
                       List<String> oldItemIds,
                       String itemId,
                       String developerPayload) throws BillingException {

        mRequestCode = requestCode;
        Bundle bundle = getBuyIntent(service, oldItemIds, itemId, developerPayload);
        PendingIntent intent = getPendingIntent(activity, bundle);

        startBuyIntent(activity, intent, requestCode);
    }

    private Bundle getBuyIntent(IInAppBillingService service, List<String> oldItemIds,
                                String itemId, String developerPayload) throws BillingException {
        try {
            // Purchase an item
            if (oldItemIds == null || oldItemIds.isEmpty()) {
                return service.getBuyIntent(
                        mApiVersion, mPackageName, itemId, mItemType, developerPayload);
            }
            // Upgrade/downgrade of subscriptions must be done on api version 5
            // See https://developer.android.com/google/play/billing/billing_reference.html#upgrade-getBuyIntentToReplaceSkus
            return service.getBuyIntentToReplaceSkus(
                    BillingApi.VERSION_5.getValue(),
                    mPackageName,
                    oldItemIds,
                    itemId,
                    mItemType,
                    developerPayload);

        } catch (RemoteException e) {
            throw new BillingException(Constants.ERROR_REMOTE_EXCEPTION, e.getMessage());
        }
    }

    private PendingIntent getPendingIntent(Activity activity, Bundle bundle) throws BillingException {
        int response = Util.getResponseCodeFromBundle(bundle, mLogger);
        if (response != Constants.BILLING_RESPONSE_RESULT_OK) {
            throw new BillingException(response, Constants.ERROR_MSG_UNABLE_TO_BUY);
        }
        if (activity == null) {
            throw new BillingException(Constants.ERROR_LOST_CONTEXT,
                    Constants.ERROR_MSG_LOST_CONTEXT);
        }
        PendingIntent pendingIntent = bundle.getParcelable(Constants.RESPONSE_BUY_INTENT);
        if (pendingIntent == null) {
            throw new BillingException(Constants.ERROR_PENDING_INTENT,
                    Constants.ERROR_MSG_PENDING_INTENT);
        }
        return pendingIntent;
    }

    private void startBuyIntent(final Activity activity,
                                final PendingIntent pendingIntent,
                                int requestCode) throws BillingException {

        IntentSender sender = pendingIntent.getIntentSender();
        try {
            activity.startIntentSenderForResult(sender, requestCode, new Intent(), 0, 0, 0);

        } catch (IntentSender.SendIntentException e) {
            throw new BillingException(Constants.ERROR_SEND_INTENT_FAILED, e.getMessage());
        }
    }

    // ******************** CHECK BILLING ACTIVITY RESULT ******************** //

    public Purchase handleResult(int requestCode, int resultCode, Intent data) throws BillingException {
        if (mRequestCode != requestCode) {
            throw new BillingException(
                    Constants.ERROR_BAD_RESPONSE, Constants.ERROR_MSG_RESULT_REQUEST_CODE_INVALID);
        }
        int responseCode = Util.getResponseCodeFromIntent(data, mLogger);
        String purchaseData = data.getStringExtra(Constants.RESPONSE_INAPP_PURCHASE_DATA);
        String signature = data.getStringExtra(Constants.RESPONSE_INAPP_SIGNATURE);
        return getPurchase(resultCode, responseCode, purchaseData, signature);
    }

    private Purchase getPurchase(int resultCode, int responseCode,
                                 String purchaseData, String signature) throws BillingException {
        // Check the Billing response
        if (resultCode == Activity.RESULT_OK
                && responseCode == Constants.BILLING_RESPONSE_RESULT_OK) {
            return getPurchaseFromIntent(purchaseData, signature);
        }
        // Something happened while trying to purchase the item
        switch (resultCode) {
            case Activity.RESULT_OK:
                throw new BillingException(responseCode, Constants.ERROR_MSG_RESULT_OK);

            case Activity.RESULT_CANCELED:
                throw new BillingException(responseCode, Constants.ERROR_MSG_RESULT_CANCELED);

            default:
                throw new BillingException(resultCode, String.format(
                        Locale.US, Constants.ERROR_MSG_RESULT_UNKNOWN, resultCode));
        }
    }

    private Purchase getPurchaseFromIntent(String purchaseData, String signature) throws BillingException {

        printBillingResponse(purchaseData, signature);
        if (purchaseData == null || signature == null) {
            throw new BillingException(Constants.ERROR_PURCHASE_DATA,
                    Constants.ERROR_MSG_NULL_PURCHASE_DATA);
        }
        if (!Security.verifyPurchase(purchaseData, mLogger, mPublicKeyBase64, purchaseData, signature)) {
            throw new BillingException(Constants.ERROR_VERIFICATION_FAILED,
                    Constants.ERROR_MSG_VERIFICATION_FAILED);
        }
        try {
            return Purchase.parseJson(purchaseData, signature);
        } catch (JSONException e) {
            mLogger.e(Logger.TAG, e.getMessage(), e);
            throw new BillingException(Constants.ERROR_BAD_RESPONSE,
                    Constants.ERROR_MSG_BAD_RESPONSE);
        }
    }

    private void printBillingResponse(String purchaseData, String dataSignature) {
        mLogger.i(Logger.TAG, "------------- BILLING RESPONSE start -------------");
        mLogger.i(Logger.TAG, "Successful resultCode from purchase activity.");
        mLogger.i(Logger.TAG, String.format("Purchase data: %s", purchaseData));
        mLogger.i(Logger.TAG, String.format("Data signature: %s", dataSignature));
        mLogger.i(Logger.TAG, "------------- BILLING RESPONSE end -------------");
    }
}