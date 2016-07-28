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

public class BillingException extends Exception {

    private final int mErrorCode;
    private final String mMessage;

    public BillingException(int errorCode, String message) {
        mErrorCode = errorCode;
        mMessage = message;
    }

    /**
     * Get the error code
     * See the available codes at the Constant class
     *
     * @return
     */
    public int getErrorCode() {
        return mErrorCode;
    }

    /**
     * Get the error message
     * See the available messages at the Constant class
     *
     * @return
     */
    public String getMessage() {
        return mMessage;
    }
}