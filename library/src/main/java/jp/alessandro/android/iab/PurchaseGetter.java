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

import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;

import java.util.List;

import jp.alessandro.android.iab.logger.Logger;

class PurchaseGetter {

    private final String mPublicKeyBase64;
    private final int mApiVersion;
    private final String mPackageName;
    private final Logger mLogger;
    private final Security mSecurity;

    PurchaseGetter(BillingContext context) {
        mPublicKeyBase64 = context.getPublicKeyBase64();
        mApiVersion = context.getApiVersion();
        mPackageName = context.getContext().getPackageName();
        mLogger = context.getLogger();
        mSecurity = new Security(BuildConfig.DEBUG);
    }

    /**
     * To retrieve information about purchases made by a user from your app,
     * call the getPurchases method on the In-app Billing Version 3 service.
     * Pass in to the method the In-app Billing API version ("3"), the package name of your calling app,
     * and the purchase type ("inapp" or "subs").
     * See https://developer.android.com/google/play/billing/billing_integrate.html#QueryPurchases
     *
     * @param service  in-app billing service
     * @param itemType "inapp" or "subs"
     * @return
     * @throws BillingException
     */
    public Purchases get(IInAppBillingService service, String itemType) throws BillingException {
        Purchases purchases = new Purchases();
        String continueToken = null;
        do {
            Bundle bundle = getPurchasesBundle(service, itemType, continueToken);
            checkResponseAndAddPurchases(bundle, purchases);
            continueToken = bundle.getString(Constants.RESPONSE_INAPP_CONTINUATION_TOKEN);
        } while (!TextUtils.isEmpty(continueToken));
        return purchases;
    }

    private Bundle getPurchasesBundle(IInAppBillingService service,
                                      String itemType,
                                      String continueToken) throws BillingException {
        try {
            return service.getPurchases(mApiVersion, mPackageName, itemType, continueToken);
        } catch (RemoteException e) {
            throw new BillingException(Constants.ERROR_REMOTE_EXCEPTION, e.getMessage());
        }
    }

    private void checkResponseAndAddPurchases(Bundle bundle, Purchases purchases) throws BillingException {
        int response = ResponseExtractor.fromBundle(bundle, mLogger);

        if (response != Constants.BILLING_RESPONSE_RESULT_OK) {
            throw new BillingException(response, Constants.ERROR_MSG_GET_PURCHASES);
        }

        List<String> purchaseList = extractPurchaseList(bundle);
        List<String> signatureList = extractSignatureList(bundle);

        if (purchaseList.size() != signatureList.size()) {
            throw new BillingException(
                    Constants.ERROR_PURCHASE_DATA, Constants.ERROR_MSG_GET_PURCHASES_DIFFERENT_SIZE);
        }
        addAllPurchases(purchaseList, signatureList, purchases);
    }

    private List<String> extractPurchaseList(Bundle bundle) throws BillingException {
        List<String> purchaseList = bundle.getStringArrayList(Constants.RESPONSE_INAPP_PURCHASE_LIST);

        if (purchaseList == null) {
            throw new BillingException(
                    Constants.ERROR_PURCHASE_DATA, Constants.ERROR_MSG_GET_PURCHASES_DATA_LIST);
        }
        return purchaseList;
    }

    private List<String> extractSignatureList(Bundle bundle) throws BillingException {
        List<String> signatureList = bundle.getStringArrayList(Constants.RESPONSE_INAPP_SIGNATURE_LIST);

        if (signatureList == null) {
            throw new BillingException(
                    Constants.ERROR_PURCHASE_DATA, Constants.ERROR_MSG_GET_PURCHASES_SIGNATURE_LIST);
        }
        return signatureList;
    }

    private void addAllPurchases(List<String> purchaseList,
                                 List<String> signatureList,
                                 Purchases purchases) throws BillingException {
        int errors = 0;
        for (int i = 0; i < purchaseList.size(); i++) {
            String purchaseData = purchaseList.get(i);
            String signature = signatureList.get(i);
            if (!verifyBeforeAddPurchase(purchaseData, signature, purchases)) {
                errors++;
            }
        }
        if (errors > 0) {
            throw new BillingException(
                    Constants.ERROR_PURCHASE_DATA, Constants.ERROR_MSG_GET_PURCHASE_VERIFICATION_FAILED);
        }
    }

    private boolean verifyBeforeAddPurchase(String purchaseData,
                                            String signature,
                                            Purchases purchases) throws BillingException {

        if (mSecurity.verifyPurchase(mLogger, mPublicKeyBase64, purchaseData, signature)) {
            addPurchase(purchaseData, signature, purchases);
            return true;
        }
        printPurchaseVerificationFailed(purchaseData, signature);
        return false;
    }

    private void addPurchase(String purchaseData,
                             String signature,
                             Purchases purchases) throws BillingException {
        Purchase purchase;
        try {
            purchase = Purchase.parseJson(purchaseData, signature);
        } catch (JSONException e) {
            mLogger.e(Logger.TAG, e.getMessage(), e);
            throw new BillingException(Constants.ERROR_BAD_RESPONSE, Constants.ERROR_MSG_BAD_RESPONSE);
        }
        purchases.put(purchase);
    }

    private void printPurchaseVerificationFailed(String purchaseData, String dataSignature) {
        mLogger.e(Logger.TAG, "------------- BILLING GET PURCHASES start -------------");
        mLogger.e(Logger.TAG, Constants.ERROR_MSG_GET_PURCHASE_VERIFICATION_FAILED_WITH_PARAMS);
        mLogger.e(Logger.TAG, String.format("Purchase data: %s", purchaseData));
        mLogger.e(Logger.TAG, String.format("Data signature: %s", dataSignature));
        mLogger.e(Logger.TAG, "------------- BILLING GET PURCHASES end -------------");
    }
}