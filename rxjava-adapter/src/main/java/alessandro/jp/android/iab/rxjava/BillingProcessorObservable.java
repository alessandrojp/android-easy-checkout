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

package alessandro.jp.android.iab.rxjava;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import jp.alessandro.android.iab.BillingContext;
import jp.alessandro.android.iab.BillingException;
import jp.alessandro.android.iab.BillingProcessor;
import jp.alessandro.android.iab.ItemList;
import jp.alessandro.android.iab.PurchaseList;
import jp.alessandro.android.iab.PurchaseType;
import jp.alessandro.android.iab.handler.ConsumeItemHandler;
import jp.alessandro.android.iab.handler.InventoryHandler;
import jp.alessandro.android.iab.handler.ItemDetailListHandler;
import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;
import rx.Emitter;
import rx.Observable;
import rx.functions.Action1;

/**
 * Created by Alessandro Yuichi Okimoto on 2016/11/24.
 */

public class BillingProcessorObservable {

    private final BillingProcessor mBillingProcessor;

    public BillingProcessorObservable(BillingContext context, PurchaseHandler purchaseHandler) {
        mBillingProcessor = new BillingProcessor(context, purchaseHandler);
    }

    /**
     * Checks if the in-app billing service is available
     *
     * @param context application context
     * @return true if it is available
     */
    public static boolean isServiceAvailable(Context context) {
        return BillingProcessor.isServiceAvailable(context);
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
     */
    public Observable<PurchaseResponse> startPurchase(final Activity activity,
                                                      final int requestCode,
                                                      final String itemId,
                                                      final PurchaseType purchaseType,
                                                      final String developerPayload) {

        return Observable.fromEmitter(new Action1<Emitter<PurchaseResponse>>() {
            @Override
            public void call(final Emitter<PurchaseResponse> emitter) {
                mBillingProcessor.startPurchase(activity,
                        requestCode,
                        itemId,
                        purchaseType,
                        developerPayload,
                        new StartActivityHandler() {
                            @Override
                            public void onSuccess() {
                                emitter.onNext(null);
                                emitter.onCompleted();
                            }

                            @Override
                            public void onError(BillingException e) {
                                emitter.onError(e);
                            }
                        });
            }
        }, Emitter.BackpressureMode.LATEST);
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
     */
    public Observable<PurchaseResponse> updateSubscription(final Activity activity,
                                                           final int requestCode,
                                                           final List<String> oldItemIds,
                                                           final String itemId,
                                                           final String developerPayload) {

        return Observable.fromEmitter(new Action1<Emitter<PurchaseResponse>>() {
            @Override
            public void call(final Emitter<PurchaseResponse> emitter) {
                mBillingProcessor.updateSubscription(activity, requestCode, oldItemIds, itemId, developerPayload,
                        new StartActivityHandler() {
                            @Override
                            public void onSuccess() {
                                emitter.onNext(null);
                                emitter.onCompleted();
                            }

                            @Override
                            public void onError(BillingException e) {
                                emitter.onError(e);
                            }
                        });
            }
        }, Emitter.BackpressureMode.LATEST);
    }

    /**
     * Consumes previously purchased item to be purchased again
     * This will be executed from Work Thread
     * See http://developer.android.com/google/play/billing/billing_integrate.html#Consume
     *
     * @param itemId consumable item id
     */
    public Observable<Void> consume(final String itemId) {
        return Observable.fromEmitter(new Action1<Emitter<Void>>() {
            @Override
            public void call(final Emitter<Void> emitter) {
                mBillingProcessor.consume(itemId, new ConsumeItemHandler() {
                    @Override
                    public void onSuccess(String itemId) {
                        emitter.onNext(null);
                        emitter.onCompleted();
                    }

                    @Override
                    public void onError(BillingException e) {
                        emitter.onError(e);
                    }
                });
            }
        }, Emitter.BackpressureMode.LATEST);
    }

    /**
     * Get the information about inventory of purchases made by a user from your app
     * This method will get all the purchases even if there are more than 500
     * This will be executed from Work Thread
     * See http://developer.android.com/google/play/billing/billing_integrate.html#QueryPurchases
     *
     * @param purchaseType IN_APP or SUBSCRIPTION
     */
    public Observable<PurchaseList> getInventory(final PurchaseType purchaseType) {
        return Observable.fromEmitter(new Action1<Emitter<PurchaseList>>() {
            @Override
            public void call(final Emitter<PurchaseList> emitter) {
                mBillingProcessor.getInventory(purchaseType, new InventoryHandler() {
                    @Override
                    public void onSuccess(PurchaseList purchaseList) {
                        emitter.onNext(purchaseList);
                        emitter.onCompleted();
                    }

                    @Override
                    public void onError(BillingException e) {
                        emitter.onError(e);
                    }
                });
            }
        }, Emitter.BackpressureMode.LATEST);
    }

    /**
     * Get a list of available SKUs details
     * This will be executed from Work Thread
     * See http://developer.android.com/google/play/billing/billing_integrate.html#QueryDetails
     *
     * @param purchaseType IN_APP or SUBSCRIPTION
     * @param itemIds      list of SKU ids to be loaded
     */
    public Observable<ItemList> getItemDetailList(final PurchaseType purchaseType, final ArrayList<String> itemIds) {
        return Observable.fromEmitter(new Action1<Emitter<ItemList>>() {
            @Override
            public void call(final Emitter<ItemList> emitter) {
                mBillingProcessor.getItemDetailList(purchaseType, itemIds, new ItemDetailListHandler() {
                    @Override
                    public void onSuccess(ItemList itemList) {
                        emitter.onNext(itemList);
                        emitter.onCompleted();
                    }

                    @Override
                    public void onError(BillingException e) {
                        emitter.onError(e);
                    }
                });
            }
        }, Emitter.BackpressureMode.LATEST);
    }

    /**
     * Checks the purchase response from Google
     * The result will be sent through PurchaseHandler
     * This method MUST be called from UI Thread
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @return
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return mBillingProcessor.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Release the handlers
     * By releasing it will not cancel the purchase process
     * since the purchase process is not controlled by the app.
     * Once you release it, you MUST to create a new instance
     */
    public void release() {
        mBillingProcessor.release();
    }
}