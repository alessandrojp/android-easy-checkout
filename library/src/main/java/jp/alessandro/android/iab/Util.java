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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jp.alessandro.android.iab.logger.Logger;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

class Util {

    private Util() {
    }

    public static BillingContext newBillingContext(Context context) {
        return new BillingContext(context, Constants.TEST_PUBLIC_KEY_BASE_64, BillingApi.VERSION_3);
    }

    public static Intent newOkIntent() {
        return newIntent(0, Constants.TEST_JSON_RECEIPT, Security.signData(Constants.TEST_JSON_RECEIPT));
    }

    public static Intent newIntent(int responseCode, String data, String signature) {
        final Intent intent = new Intent();
        intent.putExtra(Constants.RESPONSE_CODE, responseCode);
        intent.putExtra(Constants.RESPONSE_INAPP_PURCHASE_DATA, data);
        intent.putExtra(Constants.RESPONSE_INAPP_SIGNATURE, signature);
        return intent;
    }

    public static ArrayList<String> createInvalidSignatureRandomlyArray(List<String> purchaseData) {
        int size = purchaseData.size();
        int randomIndex = getRandomIndex(size);
        ArrayList<String> signatures = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (i == randomIndex) {
                signatures.add("signature");
            } else {
                signatures.add(Security.signData(purchaseData.get(i)));
            }
        }
        return signatures;
    }

    public static ArrayList<String> createSignatureArray(List<String> purchaseData) {
        ArrayList<String> signatures = new ArrayList<>();
        for (String data : purchaseData) {
            signatures.add(Security.signData(data));
        }
        return signatures;
    }

    public static Bundle createPurchaseBundle(int responseCode, int startIndex, int size, String continuationString) {
        ArrayList<String> purchaseArray = Util.createPurchaseJsonArray(startIndex, size);
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.RESPONSE_CODE, responseCode);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_PURCHASE_LIST, purchaseArray);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_SIGNATURE_LIST, createSignatureArray(purchaseArray));
        bundle.putString(Constants.RESPONSE_INAPP_CONTINUATION_TOKEN, continuationString);

        return bundle;
    }

    public static Bundle createPurchaseWithNoTokenBundle(int responseCode, int startIndex, int size, String continuationString) {
        ArrayList<String> purchaseArray = Util.createPurchaseWithNoTokenJsonArray(startIndex, size);
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.RESPONSE_CODE, responseCode);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_PURCHASE_LIST, purchaseArray);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_SIGNATURE_LIST, createSignatureArray(purchaseArray));
        bundle.putString(Constants.RESPONSE_INAPP_CONTINUATION_TOKEN, continuationString);

        return bundle;
    }

    public static ArrayList<String> createPurchaseJsonArray(int startIndex, int size) {
        ArrayList<String> purchases = new ArrayList<>();
        for (int i = startIndex; i < (size + startIndex); i++) {
            String json = String.format(Locale.ENGLISH, Constants.TEST_JSON_RECEIPT, i);
            purchases.add(json);
        }
        return purchases;
    }

    public static ArrayList<String> createPurchaseWithNoTokenJsonArray(int startIndex, int size) {
        ArrayList<String> purchases = new ArrayList<>();
        for (int i = startIndex; i < (size + startIndex); i++) {
            String json = String.format(Locale.ENGLISH, Constants.TEST_JSON_RECEIPT_NO_TOKEN, i);
            purchases.add(json);
        }
        return purchases;
    }

    public static ArrayList<String> createPurchaseJsonBrokenArray() {
        ArrayList<String> data = new ArrayList<>();
        data.add(Constants.TEST_JSON_BROKEN);
        data.add(String.format(Locale.ENGLISH, Constants.TEST_JSON_RECEIPT, 0));
        return data;
    }

    public static ArrayList<String> createSkuItemDetailsJsonArray(int size) {
        ArrayList<String> purchases = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String json = String.format(Locale.ENGLISH, Constants.SKU_DETAIL_JSON, i);
            purchases.add(json);
        }
        return purchases;
    }

    public static ArrayList<String> createSkuDetailsJsonBrokenArray() {
        ArrayList<String> data = new ArrayList<>();
        data.add(String.format(Locale.ENGLISH, Constants.SKU_DETAIL_JSON, 0));
        data.add(Constants.TEST_JSON_BROKEN);
        data.add(String.format(Locale.ENGLISH, Constants.SKU_DETAIL_JSON, 2));
        return data;
    }

    public static int getRandomIndex(int size) {
        return (int) (Math.random() * size);
    }

    public static int getResponseCodeFromBundle(Bundle bundle, Logger logger) throws BillingException {
        if (bundle == null) {
            logger.e(Logger.TAG, Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL);
            throw new BillingException(
                    Constants.ERROR_UNEXPECTED_TYPE, Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL);
        }
        return Util.getResponseCode(bundle.get(Constants.RESPONSE_CODE), logger);
    }

    public static int getResponseCodeFromIntent(Intent intent, Logger logger) throws BillingException {
        if (intent == null) {
            throw new BillingException(
                    Constants.ERROR_UNEXPECTED_TYPE, Constants.ERROR_MSG_RESULT_NULL_INTENT);
        }
        return Util.getResponseCode(intent.getExtras().get(Constants.RESPONSE_CODE), logger);
    }

    /**
     * Workaround to bug where sometimes response codes come as Long instead of Integer
     */
    private static int getResponseCode(Object obj, Logger logger) throws BillingException {
        if (obj == null) {
            logger.e(Logger.TAG,
                    "Intent with no response code, assuming there is no problem (known issue).");
            return Constants.BILLING_RESPONSE_RESULT_OK;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).intValue();
        }
        if (obj instanceof Long) {
            return (int) ((Long) obj).longValue();
        }
        logger.e(Logger.TAG, "Unexpected type for intent response code.");
        throw new BillingException(
                Constants.ERROR_UNEXPECTED_TYPE, Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE);
    }
}