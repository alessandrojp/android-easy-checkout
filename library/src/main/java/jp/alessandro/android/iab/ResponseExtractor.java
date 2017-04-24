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

import android.content.Intent;
import android.os.Bundle;

import jp.alessandro.android.iab.logger.Logger;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

class ResponseExtractor {

    private ResponseExtractor() {
    }

    public static int fromBundle(Bundle bundle, Logger logger) throws BillingException {
        if (bundle == null) {
            logger.e(Logger.TAG, Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL);
            throw new BillingException(
                    Constants.ERROR_UNEXPECTED_TYPE, Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL);
        }
        return ResponseExtractor.getResponseCode(bundle.get(Constants.RESPONSE_CODE), logger);
    }

    public static int fromIntent(Intent intent, Logger logger) throws BillingException {
        if (intent == null) {
            throw new BillingException(
                    Constants.ERROR_UNEXPECTED_TYPE, Constants.ERROR_MSG_RESULT_NULL_INTENT);
        }
        return ResponseExtractor.getResponseCode(intent.getExtras().get(Constants.RESPONSE_CODE), logger);
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