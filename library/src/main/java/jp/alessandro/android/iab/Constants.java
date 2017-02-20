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

public class Constants {

    private Constants() {
    }

    // ******************** BILLING SETTINGS ******************** //
    static final String ITEM_TYPE_INAPP = "inapp";
    static final String ITEM_TYPE_SUBSCRIPTION = "subs";
    static final String VENDING_PACKAGE = "com.android.vending";
    static final String ACTION_BILLING_SERVICE_BIND = "com.android.vending.billing.InAppBillingService.BIND";


    // ******************** GOOGLE BILLING RESPONSES CODES ******************** //
    // Success
    public static final int BILLING_RESPONSE_RESULT_OK = 0;

    // User pressed back or canceled a dialog
    public static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;

    // Network connection is down
    public static final int BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE = 2;

    // Billing API version is not supported for the type requested
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;

    // Requested product is not available for purchase
    public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;

    // Invalid arguments provided to the API
    public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;

    // Fatal error during the API action
    public static final int BILLING_RESPONSE_RESULT_ERROR = 6;

    // Failure to purchase since item is already owned
    public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;

    // Failure to consume since item is not owned
    public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;


    // ******************** BILLING RESPONSES KEYS ******************** //
    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    public static final String RESPONSE_ITEM_ID_LIST = "ITEM_ID_LIST";
    public static final String RESPONSE_DETAILS_LIST = "DETAILS_LIST";
    public static final String RESPONSE_BUY_INTENT = "BUY_INTENT";
    public static final String RESPONSE_INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";
    public static final String RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    public static final String RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE";
    public static final String RESPONSE_INAPP_PURCHASE_LIST = "INAPP_PURCHASE_DATA_LIST";
    public static final String RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";


    // ******************** BILLING INTERNAL ERROR CODES ******************** //
    public static final int ERROR_REMOTE_EXCEPTION = -101;
    public static final int ERROR_BAD_RESPONSE = -102;
    public static final int ERROR_PURCHASE_FLOW_ALREADY_EXISTS = -103;
    public static final int ERROR_LOST_CONTEXT = -104;
    public static final int ERROR_PURCHASE_DATA = -105;
    public static final int ERROR_PENDING_INTENT = -106;
    public static final int ERROR_PURCHASES_NOT_SUPPORTED = -107;
    public static final int ERROR_SUBSCRIPTIONS_NOT_SUPPORTED = -108;
    public static final int ERROR_SEND_INTENT_FAILED = -109;
    public static final int ERROR_VERIFICATION_FAILED = -110;
    public static final int ERROR_UNEXPECTED_TYPE = -111;
    public static final int ERROR_BIND_SERVICE_FAILED_EXCEPTION = -112;


    // ******************** BILLING ERROR MESSAGES ******************** //
    public static final String ERROR_MSG_BAD_RESPONSE = "Failed to parse the purchase data. Please check the log for more info.";
    public static final String ERROR_MSG_BIND_SERVICE_FAILED = "Failed to bind In-App Billing service. " +
            "Have you checked if this device supports In-App Billing? " +
            "If not, you can check if it is available calling isServiceAvailable. " +
            "See the documentation for more information or the logs.";

    @SuppressWarnings("checkstyle:linelength")
    public static final String ERROR_MSG_BIND_SERVICE_FAILED_NPE = "NullPointerException while trying to bind service. Please check the log for more info.";
    @SuppressWarnings("checkstyle:linelength")
    public static final String ERROR_MSG_BIND_SERVICE_FAILED_ILLEGAL_ARGUMENT = "IllegalArgumentException while trying to bind service. Please check the log for more info.";
    @SuppressWarnings("checkstyle:linelength")
    public static final String ERROR_MSG_BIND_SERVICE_FAILED_SERVICE_NULL = "onServiceConnected was called but InAppBillingService is null.";

    public static final String ERROR_MSG_CONSUME = "Error while trying  to consume item.";
    public static final String ERROR_MSG_GET_PURCHASES = "Error while trying to get purchases.";
    public static final String ERROR_MSG_GET_PURCHASES_DATA_LIST = "Purchase list is null.";
    public static final String ERROR_MSG_GET_PURCHASES_DIFFERENT_SIZE = "Purchase and Signature have different sizes.";
    public static final String ERROR_MSG_GET_PURCHASES_SIGNATURE_LIST = "Signature list is null.";
    @SuppressWarnings("checkstyle:linelength")
    public static final String ERROR_MSG_GET_PURCHASE_VERIFICATION_FAILED_WITH_PARAMS = "***FAILED*** Purchase signature verification failed. Not adding item. PurchaseData: %s, signature: %s.";
    @SuppressWarnings("checkstyle:linelength")
    public static final String ERROR_MSG_GET_PURCHASE_VERIFICATION_FAILED = "***FAILED*** Failed to verify if the purchase is valid or not. Please check the log for more info.";
    public static final String ERROR_MSG_GET_SKU_DETAILS = "Error while trying to get sku details.";
    public static final String ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL = "Response item details list is null.";
    public static final String ERROR_MSG_LIBRARY_ALREADY_RELEASED = "The library was released. Please generate a new instance of BillingProcessor.";
    public static final String ERROR_MSG_LOST_CONTEXT = "Context is null.";
    public static final String ERROR_MSG_METHOD_MUST_BE_CALLED_ON_UI_THREAD = "Must be called from UI Thread.";
    public static final String ERROR_MSG_NULL_PURCHASE_DATA = "IAB returned null purchaseData or signature.";
    public static final String ERROR_MSG_PENDING_INTENT = "Pending intent is null. Probably a BUG.";
    public static final String ERROR_MSG_PURCHASE_FLOW_ALREADY_EXISTS = "Purchase flow already exists. RequestCode: %d.";
    public static final String ERROR_MSG_PURCHASES_NOT_SUPPORTED = "Purchases are not supported on this device.";
    public static final String ERROR_MSG_PURCHASE_OR_TOKEN_NULL = "Purchase data or token is null.";
    public static final String ERROR_MSG_RESULT_NULL_INTENT = "IAB result returned a null intent data.";
    public static final String ERROR_MSG_RESULT_REQUEST_CODE_INVALID = "An invalid requestCode was given.";
    public static final String ERROR_MSG_RESULT_OK = "Problem while trying to purchase an item.";
    public static final String ERROR_MSG_RESULT_CANCELED = "The purchasing has canceled.";
    public static final String ERROR_MSG_RESULT_UNKNOWN = "Unknown result code: %d";
    public static final String ERROR_MSG_SUBSCRIPTIONS_NOT_SUPPORTED = "Subscriptions are not supported on this device.";
    public static final String ERROR_MSG_UNABLE_TO_BUY = "Unable to buy the item.";
    public static final String ERROR_MSG_VERIFICATION_FAILED = "Signature verification has failed.";
    public static final String ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE = "***BUG*** Unexpected type for bundle response code.";
    public static final String ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL = "***BUG*** Bundle response is null.";
    public static final String ERROR_MSG_UPDATE_ARGUMENT_MISSING = "Argument oldItemList cannot be null or empty.";


    // ******************** BILLING TESTS ******************** //
    static final String TEST_ORDER_ID = "GPA.1234-5678-9012-34567";
    static final String TEST_PACKAGE_NAME = "jp.alessandro.android.iab";
    static final String TEST_PRODUCT_ID = "android.test.purchased";
    static final String TEST_PURCHASE_TIME = "1345678900000";
    static final String TEST_DEVELOPER_PAYLOAD = "optional_developer_payload";
    static final String TEST_PURCHASE_TOKEN = "opaque-token-up-to-1000-characters";
    @SuppressWarnings("checkstyle:linelength")
    static final String TEST_PUBLIC_KEY_BASE_64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7SEtV7WT1vJKdS1fBgskYk+c8j6YUa6kz8NwLbD7EkKGh+0ocSmsde4BewrQDijHC0z6Cxs3s8Kks2JC75NTZUvRQRN5T19Po2owTXTrkT5+Zh2nt5/0lj7RnMyB6qYMeVebDh4oUmj4YkLdQ3QjOpLjGep1xjIunOvJrpMiNkQuRl3ENBbkwEbDKzSquXXMngjfkx2PyHfirbE2dDVXkG85G542KSBfOHF1AQpEO7hiRgz8b5JTuSe4oOdYc11WG4bNxnLpcUeh8xwE9txcipDrz6cUFfb6D3lL8zPIzyZxiwIr0+G0O7ise+vIMaP0JOA891eqruBVEI7WPCyT0QIDAQAB";
    static final String TEST_JSON_RECEIPT = "{" +
            "\"orderId\":\"" + TEST_ORDER_ID + "\"," +
            "\"packageName\":\"" + TEST_PACKAGE_NAME + "\"," +
            "\"productId\":\"" + TEST_PRODUCT_ID + "_%d\"," +
            "\"purchaseTime\":" + TEST_PURCHASE_TIME + "," +
            "\"purchaseState\":0," +
            "\"developerPayload\":\"" + TEST_DEVELOPER_PAYLOAD + "\"," +
            "\"purchaseToken\":\"" + TEST_PURCHASE_TOKEN + "\"," +
            "\"autoRenewing\":true}";

    static final String TEST_JSON_RECEIPT_NO_TOKEN = "{" +
            "\"orderId\":\"" + TEST_ORDER_ID + "\"," +
            "\"packageName\":\"" + TEST_PACKAGE_NAME + "\"," +
            "\"productId\":\"" + TEST_PRODUCT_ID + "_%d\"," +
            "\"purchaseTime\":" + TEST_PURCHASE_TIME + "," +
            "\"purchaseState\":0," +
            "\"developerPayload\":\"" + TEST_DEVELOPER_PAYLOAD + "\"," +
            "\"autoRenewing\":true}";

    static final String TEST_JSON_BROKEN = "{\"productId\":\"\"";

    static final String SKU_DETAIL_JSON = "{" +
            "\"productId\": \"" + TEST_PRODUCT_ID + "_%d\"," +
            "\"type\": \"subs\"," +
            "\"price\": \"¥1080\"," +
            "\"price_amount_micros\": \"10800000\"," +
            "\"price_currency_code\": \"JPY\"," +
            "\"title\": \"Test Product\"," +
            "\"description\": \"Fast and easy use Android In-App Billing\"}";

    static final String TYPE_IN_APP = "inapp";
    static final String TYPE_SUBSCRIPTION = "subs";
}