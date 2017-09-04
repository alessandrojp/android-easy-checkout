/*
 * Copyright (C) 2016 Alessandro Yuichi Okimoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact email: alessandro@alessandro.jp
 */

package jp.alessandro.android.iab.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jp.alessandro.android.iab.BillingApi;
import jp.alessandro.android.iab.BillingContext;
import jp.alessandro.android.iab.Constants;
import jp.alessandro.android.iab.PurchaseType;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/26.
 */

public class DataConverter {

    // ******************** BILLING TESTS ******************** //
    public static final String TEST_ORDER_ID = "GPA.1234-5678-9012-34567";
    public static final String TEST_PACKAGE_NAME = "jp.alessandro.android.iab";
    public static final String TEST_PRODUCT_ID = "android.test.purchased";
    public static final String TEST_PURCHASE_TIME = "1345678900000";
    public static final String TEST_DEVELOPER_PAYLOAD = "optional_developer_payload";
    public static final String TEST_PURCHASE_TOKEN = "opaque-token-up-to-1000-characters";
    @SuppressWarnings("checkstyle:linelength")
    public static final String TEST_PUBLIC_KEY_BASE_64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7SEtV7WT1vJKdS1fBgskYk+c8j6YUa6kz8NwLbD7EkKGh+0ocSmsde4BewrQDijHC0z6Cxs3s8Kks2JC75NTZUvRQRN5T19Po2owTXTrkT5+Zh2nt5/0lj7RnMyB6qYMeVebDh4oUmj4YkLdQ3QjOpLjGep1xjIunOvJrpMiNkQuRl3ENBbkwEbDKzSquXXMngjfkx2PyHfirbE2dDVXkG85G542KSBfOHF1AQpEO7hiRgz8b5JTuSe4oOdYc11WG4bNxnLpcUeh8xwE9txcipDrz6cUFfb6D3lL8zPIzyZxiwIr0+G0O7ise+vIMaP0JOA891eqruBVEI7WPCyT0QIDAQAB";
    public static final String TEST_JSON_RECEIPT = "{" +
            "\"orderId\":\"" + TEST_ORDER_ID + "\"," +
            "\"packageName\":\"" + TEST_PACKAGE_NAME + "\"," +
            "\"productId\":\"" + TEST_PRODUCT_ID + "_%d\"," +
            "\"purchaseTime\":" + TEST_PURCHASE_TIME + "," +
            "\"purchaseState\":0," +
            "\"developerPayload\":\"" + TEST_DEVELOPER_PAYLOAD + "\"," +
            "\"purchaseToken\":\"" + TEST_PURCHASE_TOKEN + "\"," +
            "\"autoRenewing\":true}";

    public static final String TEST_JSON_RECEIPT_AUTO_RENEWING_FALSE = "{" +
            "\"orderId\":\"" + TEST_ORDER_ID + "\"," +
            "\"packageName\":\"" + TEST_PACKAGE_NAME + "\"," +
            "\"productId\":\"" + TEST_PRODUCT_ID + "_%d\"," +
            "\"purchaseTime\":" + TEST_PURCHASE_TIME + "," +
            "\"purchaseState\":0," +
            "\"developerPayload\":\"" + TEST_DEVELOPER_PAYLOAD + "\"," +
            "\"purchaseToken\":\"" + TEST_PURCHASE_TOKEN + "\"," +
            "\"autoRenewing\":false}";

    public static final String TEST_JSON_RECEIPT_NO_TOKEN = "{" +
            "\"orderId\":\"" + TEST_ORDER_ID + "\"," +
            "\"packageName\":\"" + TEST_PACKAGE_NAME + "\"," +
            "\"productId\":\"" + TEST_PRODUCT_ID + "_%d\"," +
            "\"purchaseTime\":" + TEST_PURCHASE_TIME + "," +
            "\"purchaseState\":0," +
            "\"developerPayload\":\"" + TEST_DEVELOPER_PAYLOAD + "\"," +
            "\"autoRenewing\":true}";

    public static final String TEST_JSON_BROKEN = "{\"productId\":\"\"";

    public static final String SKU_DETAILS_JSON = "{" +
            "\"productId\": \"" + TEST_PRODUCT_ID + "_%d\"," +
            "\"type\": \"inapp\"," +
            "\"price\": \"¥1080\"," +
            "\"price_amount_micros\": \"10800000\"," +
            "\"price_currency_code\": \"JPY\"," +
            "\"title\": \"Test Product\"," +
            "\"description\": \"Fast and easy use Android In-App Billing\"}";

    public static final String SKU_SUBSCRIPTION_DETAILS_JSON = "{" +
            "\"productId\": \"" + TEST_PRODUCT_ID + "_%d\"," +
            "\"type\": \"subs\"," +
            "\"price\": \"¥1080\"," +
            "\"price_amount_micros\": \"10800000\"," +
            "\"price_currency_code\": \"JPY\"," +
            "\"title\": \"Test Product\"," +
            "\"description\": \"Fast and easy use Android In-App Billing\"," +
            "\"subscriptionPeriod\": \"P1M\"," +
            "\"freeTrialPeriod\": \"P7D\"," +
            "\"introductoryPrice\": \"¥1080\"," +
            "\"introductoryPriceAmountMicros\": \"10800000\"," +
            "\"introductoryPricePeriod\": \"P1M\"," +
            "\"introductoryPriceCycles\": 3}";

    private final DataSigner mDataSigner;

    private String mKeyFactoryAlgorithm;
    private String mSignatureAlgorithm;

    public DataConverter(String keyFactoryAlgorithm, String signatureAlgorithm) {
        mKeyFactoryAlgorithm = keyFactoryAlgorithm;
        mSignatureAlgorithm = signatureAlgorithm;
        mDataSigner = new DataSigner();
    }

    public BillingContext newBillingContext(Context context) {
        return new BillingContext(context, TEST_PUBLIC_KEY_BASE_64, BillingApi.VERSION_3);
    }

    public Intent newOkIntent(int index) {
        String jsonReceipt = String.format(Locale.ENGLISH, TEST_JSON_RECEIPT, index);
        return newIntent(0, jsonReceipt, mDataSigner.sign(jsonReceipt, mKeyFactoryAlgorithm, mSignatureAlgorithm));
    }

    public Intent newIntent(String data, String signature) {
        final Intent intent = new Intent();
        intent.putExtra(Constants.RESPONSE_INAPP_PURCHASE_DATA, data);
        intent.putExtra(Constants.RESPONSE_INAPP_SIGNATURE, signature);
        return intent;
    }

    public Intent newIntent(int responseCode, String data, String signature) {
        final Intent intent = new Intent();
        intent.putExtra(Constants.RESPONSE_CODE, responseCode);
        intent.putExtra(Constants.RESPONSE_INAPP_PURCHASE_DATA, data);
        intent.putExtra(Constants.RESPONSE_INAPP_SIGNATURE, signature);
        return intent;
    }

    public ArrayList<String> convertToItemIdArrayList(int size) {
        ArrayList<String> itemIds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String itemId = String.format(Locale.ENGLISH, "%s_%d", TEST_PRODUCT_ID, i);
            itemIds.add(itemId);
        }
        return itemIds;
    }

    public ArrayList<String> convertToInvalidSignatureRandomlyArrayList(List<String> purchaseData) {
        int size = purchaseData.size();
        int randomIndex = getRandomIndex(size);
        ArrayList<String> signatures = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (i == randomIndex) {
                signatures.add("signature");
            } else {
                signatures.add(mDataSigner.sign(purchaseData.get(i), mKeyFactoryAlgorithm, mSignatureAlgorithm));
            }
        }
        return signatures;
    }

    public ArrayList<String> convertToSignatureArrayList(List<String> purchaseData) {
        ArrayList<String> signatures = new ArrayList<>();
        for (String data : purchaseData) {
            signatures.add(mDataSigner.sign(data, mKeyFactoryAlgorithm, mSignatureAlgorithm));
        }
        return signatures;
    }

    public Bundle convertToPurchaseResponseBundle(int responseCode, int startIndex, int size, String continuationString) {
        ArrayList<String> purchaseArray = convertToPurchaseJsonArrayList(startIndex, size);
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.RESPONSE_CODE, responseCode);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_PURCHASE_LIST, purchaseArray);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_SIGNATURE_LIST, convertToSignatureArrayList(purchaseArray));
        bundle.putString(Constants.RESPONSE_INAPP_CONTINUATION_TOKEN, continuationString);

        return bundle;
    }

    public Bundle convertToPurchaseResponseWithNoTokenBundle(int responseCode,
                                                             int startIndex,
                                                             int size,
                                                             String continuationString) {

        ArrayList<String> purchaseArrayList = convertToPurchaseWithNoTokenJsonArrayList(startIndex, size);
        ArrayList<String> signatureArrayList = convertToSignatureArrayList(purchaseArrayList);

        Bundle bundle = new Bundle();
        bundle.putInt(Constants.RESPONSE_CODE, responseCode);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_PURCHASE_LIST, purchaseArrayList);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_SIGNATURE_LIST, signatureArrayList);
        bundle.putString(Constants.RESPONSE_INAPP_CONTINUATION_TOKEN, continuationString);

        return bundle;
    }

    public ArrayList<String> convertToPurchaseJsonArrayList(int startIndex, int size) {
        ArrayList<String> purchases = new ArrayList<>();
        for (int i = startIndex; i < (size + startIndex); i++) {
            String json = String.format(Locale.ENGLISH, TEST_JSON_RECEIPT, i);
            purchases.add(json);
        }
        return purchases;
    }

    public ArrayList<String> convertToPurchaseWithNoTokenJsonArrayList(int startIndex, int size) {
        ArrayList<String> purchases = new ArrayList<>();
        for (int i = startIndex; i < (size + startIndex); i++) {
            String json = String.format(Locale.ENGLISH, TEST_JSON_RECEIPT_NO_TOKEN, i);
            purchases.add(json);
        }
        return purchases;
    }

    public ArrayList<String> convertToPurchaseJsonBrokenArrayList() {
        ArrayList<String> data = new ArrayList<>();
        data.add(TEST_JSON_BROKEN);
        data.add(String.format(Locale.ENGLISH, TEST_JSON_RECEIPT, 0));
        return data;
    }

    public ArrayList<String> convertToSkuItemDetailsJsonArrayList(int size, PurchaseType type) {
        ArrayList<String> purchases = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String targetSku = type == PurchaseType.IN_APP ? SKU_DETAILS_JSON : SKU_SUBSCRIPTION_DETAILS_JSON;
            String json = String.format(Locale.ENGLISH, targetSku, i);
            purchases.add(json);
        }
        return purchases;
    }

    public ArrayList<String> convertToSkuDetailsJsonBrokenArrayList() {
        ArrayList<String> data = new ArrayList<>();
        data.add(String.format(Locale.ENGLISH, SKU_DETAILS_JSON, 0));
        data.add(TEST_JSON_BROKEN);
        data.add(String.format(Locale.ENGLISH, SKU_DETAILS_JSON, 2));
        return data;
    }

    private int getRandomIndex(int size) {
        return (int) (Math.random() * size);
    }
}