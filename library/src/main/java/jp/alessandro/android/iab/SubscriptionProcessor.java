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
import android.os.Handler;

import java.util.List;

import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;

class SubscriptionProcessor extends BaseProcessor {

    public SubscriptionProcessor(BillingContext context, PurchaseHandler purchaseHandler,
                                 Handler workHandler, Handler mainHandler) {

        super(context, Constants.ITEM_TYPE_SUBSCRIPTION, purchaseHandler, workHandler, mainHandler);
    }

    /**
     * Updates a subscription (Upgrade / Downgrade)
     * This method MUST be called from UI Thread
     * This can only be done on API version 5
     * Even if you set up to use the API version 3
     * It will automatically use API version 5
     * IMPORTANT: In some devices it may not work
     *
     * @param activity         activity calling this method
     * @param requestCode
     * @param oldItemIds       a list of item ids to be updated
     * @param itemId           new subscription item id
     * @param developerPayload optional argument to be sent back with the purchase information. It helps to identify the user
     * @param handler          callback called asynchronously
     */
    public void update(Activity activity,
                       int requestCode,
                       List<String> oldItemIds,
                       String itemId,
                       String developerPayload,
                       StartActivityHandler handler) {
        if (oldItemIds == null || oldItemIds.isEmpty()) {
            throw new IllegalArgumentException(Constants.ERROR_MSG_UPDATE_ARGUMENT_MISSING);
        }
        super.startPurchase(activity, requestCode, oldItemIds, itemId, developerPayload, handler);
    }
}