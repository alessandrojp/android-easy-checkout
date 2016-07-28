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

import java.util.List;

import jp.alessandro.android.iab.handler.PurchaseHandler;

public class SubscriptionProcessor extends BaseProcessor {

    public SubscriptionProcessor(BillingContext context) {
        super(context, Constants.ITEM_TYPE_SUBSCRIPTION);
    }

    /**
     * Purchase a subscription
     *
     * @param activity         activity calling this method
     * @param itemId           subscription item id
     * @param developerPayload optional argument to be sent back with the purchase information. It helps to identify the user
     * @param handler          callback called asynchronously
     */
    public void purchase(Activity activity, String itemId,
                         String developerPayload, PurchaseHandler handler) {
        super.purchase(activity, null, itemId, developerPayload, handler);
    }

    /**
     * Updates a subscription (Upgrade / Downgrade)
     * This can only be done on API version 5
     * Even if you set up to use the API version 3
     * It will automatically use API version 5
     * IMPORTANT: In some devices it may not work
     *
     * @param activity         activity calling this method
     * @param oldItemIds       a list of item ids to be updated
     * @param itemId           new subscription item id
     * @param developerPayload optional argument to be sent back with the purchase information. It helps to identify the user
     * @param handler          callback called asynchronously
     */
    public void update(Activity activity, List<String> oldItemIds,
                       String itemId, String developerPayload, PurchaseHandler handler) {
        if (oldItemIds == null || oldItemIds.isEmpty()) {
            throw new RuntimeException(Constants.ERROR_MSG_UPDATE_ARGUMENT_MISSING);
        }
        super.purchase(activity, oldItemIds, itemId, developerPayload, handler);
    }
}