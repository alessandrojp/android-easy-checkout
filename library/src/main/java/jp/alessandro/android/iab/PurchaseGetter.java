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

import java.util.ArrayList;

import jp.alessandro.android.iab.logger.Logger;

class PurchaseGetter {

    private final String mSignatureBase64;
    private final int mApiVersion;
    private final String mPackageName;
    private final Logger mLogger;

    PurchaseGetter(BillingContext context) {
        mSignatureBase64 = context.getSignatureBase64();
        mApiVersion = context.getApiVersion();
        mPackageName = context.getContext().getPackageName();
        mLogger = context.getLogger();
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
    public PurchaseList get(IInAppBillingService service, String itemType) throws BillingException {
        PurchaseList purchaseList = new PurchaseList();
        String continueToken = null;
        do {
            Bundle bundle = getPurchasesBundle(service, itemType, continueToken);
            checkResponse(bundle, purchaseList);
            continueToken = bundle.getString(Constants.RESPONSE_INAPP_CONTINUATION_TOKEN);
        } while (!TextUtils.isEmpty(continueToken));
        return purchaseList;
    }

    private Bundle getPurchasesBundle(IInAppBillingService service, String itemType,
                                      String continueToken) throws BillingException {
        try {
            return service.getPurchases(mApiVersion, mPackageName, itemType, continueToken);
        } catch (RemoteException e) {
            throw new BillingException(Constants.ERROR_REMOTE_EXCEPTION, e.getMessage());
        }
    }

    private void checkResponse(Bundle data, PurchaseList purchasesList) throws BillingException {
        int response = data.getInt(Constants.RESPONSE_CODE);
        if (response == Constants.BILLING_RESPONSE_RESULT_OK) {
            ArrayList<String> purchaseList =
                    data.getStringArrayList(Constants.RESPONSE_INAPP_PURCHASE_LIST);

            ArrayList<String> signatureList =
                    data.getStringArrayList(Constants.RESPONSE_INAPP_SIGNATURE_LIST);

            checkPurchaseList(purchaseList, signatureList, purchasesList);
        } else {
            throw new BillingException(response, Constants.ERROR_MSG_GET_PURCHASES);
        }
    }

    private void checkPurchaseList(ArrayList<String> purchaseList, ArrayList<String> signatureList,
                                   PurchaseList purchasesList) throws BillingException {
        if ((purchaseList == null) || (signatureList == null)) {
            throw new BillingException(Constants.ERROR_PURCHASE_DATA,
                    Constants.ERROR_MSG_GET_PURCHASES_SIGNATURE);
        }
        if (purchaseList.size() != signatureList.size()) {
            throw new BillingException(Constants.ERROR_PURCHASE_DATA,
                    Constants.ERROR_MSG_GET_PURCHASES_SIGNATURE_SIZE);
        }
        verifyAllPurchases(purchaseList, signatureList, purchasesList);
    }

    private void verifyAllPurchases(ArrayList<String> purchaseList,
                                    ArrayList<String> signatureList,
                                    PurchaseList purchasesList) throws BillingException {
        for (int i = 0; i < purchaseList.size(); i++) {
            String purchaseData = purchaseList.get(i);
            String signature = signatureList.get(i);
            verifyBeforeAddPurchase(purchasesList, purchaseData, signature);
        }
    }

    private void verifyBeforeAddPurchase(PurchaseList purchaseList, String purchaseData,
                                         String signature) throws BillingException {
        if (!TextUtils.isEmpty(purchaseData)) {
            if (Security.verifyPurchase(purchaseData, mLogger, mSignatureBase64, purchaseData, signature)) {
                addPurchase(purchaseList, purchaseData, signature);
            } else {
                mLogger.w(Logger.TAG, String.format(
                        "Purchase not valid. PurchaseData: %s, signature: %s", purchaseData, signature));
            }
        }
    }

    private void addPurchase(PurchaseList purchaseList,
                             String purchaseData,
                             String signature) throws BillingException {
        Purchase purchase;
        try {
            purchase = Purchase.parseJson(purchaseData, signature);
        } catch (JSONException e) {
            throw new BillingException(Constants.ERROR_BAD_RESPONSE, e.getMessage());
        }
        if (TextUtils.isEmpty(purchase.getToken())) {
            throw new BillingException(Constants.ERROR_PURCHASE_DATA,
                    Constants.ERROR_MSG_PURCHASE_TOKEN);
        }
        purchaseList.put(purchase);
    }
}