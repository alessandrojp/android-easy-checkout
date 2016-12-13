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

package jp.alessandro.android.iab;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

import jp.alessandro.android.iab.handler.ConsumeItemHandler;
import jp.alessandro.android.iab.handler.InventoryHandler;
import jp.alessandro.android.iab.handler.ItemDetailsHandler;
import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;

/**
 * Created by Alessandro Yuichi Okimoto on 2016/11/22.
 */

public class BillingProcessor {

    private SubscriptionProcessor mSubscriptionProcessor;
    private ItemProcessor mItemProcessor;
    private Handler mWorkHandler;
    private Handler mMainHandler;

    public BillingProcessor(BillingContext context, PurchaseHandler purchaseHandler) {
        HandlerThread thread = new HandlerThread("AndroidIabThread");
        thread.start();
        // Handler to post all actions in the library
        mWorkHandler = new Handler(thread.getLooper());
        // Handler to post all events in the library
        mMainHandler = new Handler(Looper.getMainLooper());

        mSubscriptionProcessor = new SubscriptionProcessor(context, purchaseHandler, mWorkHandler, mMainHandler);
        mItemProcessor = new ItemProcessor(context, purchaseHandler, mWorkHandler, mMainHandler);
    }

    /**
     * Checks if the in-app billing service is available
     *
     * @param context application context
     * @return true if it is available
     */
    public static boolean isServiceAvailable(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent serviceIntent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        serviceIntent.setPackage(Constants.VENDING_PACKAGE);
        List<ResolveInfo> list = packageManager.queryIntentServices(serviceIntent, 0);

        return list != null && list.size() > 0;
    }

    /**
     * Starts to purchase a consumable/non-consumable item or a subscription
     * This will be executed from UI Thread
     *
     * @param activity         activity calling this method
     * @param requestCode      request code for the billing activity
     * @param itemId           product item id
     * @param purchaseType     IN_APP or SUBSCRIPTION
     * @param developerPayload optional argument to be sent back with the purchase information. It helps to identify the user
     * @param handler          callback called asynchronously
     */
    public void startPurchase(Activity activity,
                              int requestCode,
                              String itemId,
                              PurchaseType purchaseType,
                              String developerPayload,
                              StartActivityHandler handler) {
        synchronized (this) {
            checkIfIsNotReleased();
            if (purchaseType == PurchaseType.SUBSCRIPTION) {
                mSubscriptionProcessor.startPurchase(activity, requestCode, null, itemId, developerPayload, handler);
            } else {
                mItemProcessor.startPurchase(activity, requestCode, null, itemId, developerPayload, handler);
            }
        }
    }

    /**
     * Consumes previously purchased item to be purchased again
     * This will be executed from Work Thread
     * See http://developer.android.com/google/play/billing/billing_integrate.html#Consume
     *
     * @param itemId  consumable item id
     * @param handler callback called asynchronously
     */
    public void consume(String itemId, ConsumeItemHandler handler) {
        synchronized (this) {
            checkIfIsNotReleased();
            mItemProcessor.consume(itemId, handler);
        }
    }

    /**
     * Updates a subscription (Upgrade / Downgrade)
     * This will be executed from UI Thread
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
    public void updateSubscription(Activity activity,
                                   int requestCode,
                                   List<String> oldItemIds,
                                   String itemId,
                                   String developerPayload,
                                   StartActivityHandler handler) {
        synchronized (this) {
            checkIfIsNotReleased();
            mSubscriptionProcessor.update(activity, requestCode, oldItemIds, itemId, developerPayload, handler);
        }
    }

    /**
     * Get the information about inventory of purchases made by a user from your app
     * This method will get all the purchases even if there are more than 500
     * This will be executed from Work Thread
     * See http://developer.android.com/google/play/billing/billing_integrate.html#QueryPurchases
     *
     * @param purchaseType IN_APP or SUBSCRIPTION
     * @param handler      callback called asynchronously
     */
    public void getInventory(PurchaseType purchaseType, InventoryHandler handler) {
        synchronized (this) {
            checkIfIsNotReleased();
            if (purchaseType == PurchaseType.SUBSCRIPTION) {
                mSubscriptionProcessor.getInventory(handler);
            } else {
                mItemProcessor.getInventory(handler);
            }
        }
    }

    /**
     * Get item details (SKU)
     * This will be executed from Work Thread
     * See http://developer.android.com/google/play/billing/billing_integrate.html#QueryDetails
     *
     * @param purchaseType IN_APP or SUBSCRIPTION
     * @param itemIds      list of SKU ids to be loaded
     * @param handler      callback called asynchronously
     */
    public void getItemDetails(PurchaseType purchaseType, ArrayList<String> itemIds, ItemDetailsHandler handler) {
        synchronized (this) {
            checkIfIsNotReleased();
            if (purchaseType == PurchaseType.SUBSCRIPTION) {
                mSubscriptionProcessor.getItemDetails(itemIds, handler);
            } else {
                mItemProcessor.getItemDetails(itemIds, handler);
            }
        }
    }

    /**
     * Checks the purchase response from Google
     * The result will be sent through PurchaseHandler
     * This method MUST be called from UI Thread
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @return true if the result was processed in the library
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        synchronized (this) {
            checkIfIsNotReleased();
            checkIsMainThread();
            if (mSubscriptionProcessor.onActivityResult(requestCode, resultCode, data)
                    || mItemProcessor.onActivityResult(requestCode, resultCode, data)) {
                return true;
            }
            return false;
        }
    }

    /**
     * Release the handlers
     * By releasing it will not cancel the purchase process
     * since the purchase process is not controlled by the app.
     * Once you release it, you MUST to create a new instance
     */
    public void release() {
        synchronized (this) {
            SubscriptionProcessor subscriptionProcessor = mSubscriptionProcessor;
            ItemProcessor itemProcessor = mItemProcessor;
            Handler mainThread = mMainHandler;
            Handler workHandler = mWorkHandler;

            mSubscriptionProcessor = null;
            mItemProcessor = null;
            mMainHandler = null;
            mWorkHandler = null;

            mainThread.removeCallbacksAndMessages(null);
            workHandler.removeCallbacksAndMessages(null);
            workHandler.getLooper().quit();

            subscriptionProcessor.release();
            itemProcessor.release();
        }
    }

    private void checkIfIsNotReleased() {
        if (mSubscriptionProcessor == null || mItemProcessor == null) {
            throw new IllegalStateException("The library was released. Please generate a new instance of BillingProcessor.");
        }
    }

    private void checkIsMainThread() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            return;
        }
        throw new IllegalStateException("Must be called from UI Thread.");
    }
}