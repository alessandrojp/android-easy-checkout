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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.SparseArray;

import com.android.vending.billing.IInAppBillingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jp.alessandro.android.iab.handler.ConsumeItemHandler;
import jp.alessandro.android.iab.handler.ErrorHandler;
import jp.alessandro.android.iab.handler.InventoryHandler;
import jp.alessandro.android.iab.handler.ItemDetailsHandler;
import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.PurchasesHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;
import jp.alessandro.android.iab.logger.Logger;
import jp.alessandro.android.iab.response.PurchaseResponse;

public class BillingProcessor {

    protected static final String WORK_THREAD_NAME = "AndroidEasyCheckoutThread";

    private final BillingContext mContext;
    private final SparseArray<PurchaseFlowLauncher> mPurchaseFlows;
    private final Logger mLogger;
    private final Intent mServiceIntent;

    private PurchaseHandler mPurchaseHandler;
    private Handler mMainHandler;
    private Handler mWorkHandler;
    private boolean mIsReleased;

    public BillingProcessor(BillingContext context, PurchaseHandler purchaseHandler) {
        Checker.billingProcessorArguments(context, purchaseHandler);

        mContext = context;
        mPurchaseHandler = purchaseHandler;
        mPurchaseFlows = new SparseArray<>();
        mLogger = context.getLogger();

        mServiceIntent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        mServiceIntent.setPackage(Constants.VENDING_PACKAGE);
    }

    /**
     * Check if nAppIInAppBillingService is supported on the device.
     *
     * @param context
     * @return true if it is supported
     */
    public synchronized static boolean isServiceAvailable(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent serviceIntent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        serviceIntent.setPackage(Constants.VENDING_PACKAGE);
        List<ResolveInfo> list = packageManager.queryIntentServices(serviceIntent, 0);

        return list != null && !list.isEmpty();
    }

    /**
     * Purchase a subscription
     * This will be executed from UI Thread
     *
     * @param activity         activity calling this method
     * @param requestCode
     * @param itemId           new subscription item id
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
            startPurchase(activity, requestCode, null, itemId, purchaseType, developerPayload, handler);
        }
    }

    /**
     * Method deprecated, please use consumePurchase above instead
     * Consumes previously purchased item to be purchased again
     * See http://developer.android.com/google/play/billing/billing_integrate.html#Consume
     *
     * @param itemId  consumable item id
     * @param handler callback called asynchronously
     */
    @Deprecated
    public void consume(final String itemId, final ConsumeItemHandler handler) {
        consumePurchase(itemId, handler);
    }

    /**
     * Consumes previously purchased item to be purchased again
     * See http://developer.android.com/google/play/billing/billing_integrate.html#Consume
     *
     * @param itemId  consumable item id
     * @param handler callback called asynchronously
     */
    public void consumePurchase(final String itemId, final ConsumeItemHandler handler) {
        synchronized (this) {
            checkIfIsNotReleased();
            Checker.consumePurchasesArguments(itemId, handler);

            executeInServiceOnWorkThread(new ServiceBinder.Handler() {
                @Override
                public void onBind(IInAppBillingService service) {
                    try {
                        checkIfBillingIsSupported(PurchaseType.IN_APP, service);

                        String token = getToken(service, itemId);
                        int response = service.consumePurchase(
                                mContext.getApiVersion(),
                                mContext.getContext().getPackageName(),
                                token
                        );
                        if (response != Constants.BILLING_RESPONSE_RESULT_OK) {
                            throw new BillingException(response, Constants.ERROR_MSG_CONSUME);
                        }
                        postConsumePurchaseSuccess(handler);

                    } catch (BillingException e) {
                        postOnError(e, handler);
                    } catch (RemoteException e) {
                        postOnError(new BillingException(Constants.ERROR_REMOTE_EXCEPTION, e.getMessage()), handler);
                    }
                }

                @Override
                public void onError(BillingException e) {
                    postBindServiceError(e, handler);
                }
            });
        }
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
    public void updateSubscription(Activity activity,
                                   int requestCode,
                                   List<String> oldItemIds,
                                   String itemId,
                                   String developerPayload,
                                   StartActivityHandler handler) {
        synchronized (this) {
            if (oldItemIds == null || oldItemIds.isEmpty()) {
                throw new IllegalArgumentException(Constants.ERROR_MSG_UPDATE_ARGUMENT_MISSING);
            }
            startPurchase(activity, requestCode, oldItemIds, itemId, PurchaseType.SUBSCRIPTION, developerPayload, handler);
        }
    }

    /**
     * Get item details (SKU)
     * See http://developer.android.com/google/play/billing/billing_integrate.html#QueryDetails
     *
     * @param purchaseType IN_APP or SUBSCRIPTION
     * @param handler      callback called asynchronously
     */
    public void getItemDetails(final PurchaseType purchaseType,
                               final ArrayList<String> itemIds,
                               final ItemDetailsHandler handler) {
        synchronized (this) {
            checkIfIsNotReleased();
            Checker.getItemDetailsArguments(purchaseType, itemIds, handler);

            executeInServiceOnWorkThread(new ServiceBinder.Handler() {
                @Override
                public void onBind(IInAppBillingService service) {
                    String type;
                    if (purchaseType == PurchaseType.SUBSCRIPTION) {
                        type = Constants.TYPE_SUBSCRIPTION;
                    } else {
                        type = Constants.TYPE_IN_APP;
                    }
                    try {
                        checkIfBillingIsSupported(purchaseType, service);

                        ItemGetter getter = new ItemGetter(mContext);
                        ItemDetails details = getter.get(service, type, itemIds);

                        postGetItemDetailsSuccess(details, handler);
                    } catch (BillingException e) {
                        postOnError(e, handler);
                    }
                }

                @Override
                public void onError(BillingException e) {
                    postBindServiceError(e, handler);
                }
            });
        }
    }

    /**
     * Get the information about inventory of purchases made by a user from your app
     * This method will get all the purchases even if there are more than 500
     * See http://developer.android.com/google/play/billing/billing_integrate.html#QueryPurchases
     *
     * @param purchaseType IN_APP or SUBSCRIPTION
     * @param handler      callback called asynchronously
     */
    public void getPurchases(final PurchaseType purchaseType, final PurchasesHandler handler) {
        synchronized (this) {
            checkIfIsNotReleased();
            Checker.getPurchasesArguments(purchaseType, handler);

            executeInServiceOnWorkThread(new ServiceBinder.Handler() {
                @Override
                public void onBind(IInAppBillingService service) {
                    String type;
                    if (purchaseType == PurchaseType.SUBSCRIPTION) {
                        type = Constants.TYPE_SUBSCRIPTION;
                    } else {
                        type = Constants.TYPE_IN_APP;
                    }
                    try {
                        checkIfBillingIsSupported(purchaseType, service);

                        PurchaseGetter getter = new PurchaseGetter(mContext);
                        Purchases purchases = getter.get(service, type);

                        postGetPurchasesSuccess(purchases, handler);
                    } catch (BillingException e) {
                        postOnError(e, handler);
                    }
                }

                @Override
                public void onError(BillingException e) {
                    postBindServiceError(e, handler);
                }
            });
        }
    }

    /**
     * Method deprecated, please use getPurchases above instead
     * <p>
     * Get the information about inventory of purchases made by a user from your app
     * This method will get all the purchases even if there are more than 500
     * See http://developer.android.com/google/play/billing/billing_integrate.html#QueryPurchases
     *
     * @param handler callback called asynchronously
     */
    @Deprecated
    public void getInventory(final PurchaseType purchaseType, final InventoryHandler handler) {
        synchronized (this) {
            checkIfIsNotReleased();
            Checker.getInventoryArguments(purchaseType, handler);

            executeInServiceOnWorkThread(new ServiceBinder.Handler() {
                @Override
                public void onBind(IInAppBillingService service) {
                    String type;
                    if (purchaseType == PurchaseType.SUBSCRIPTION) {
                        type = Constants.TYPE_SUBSCRIPTION;
                    } else {
                        type = Constants.TYPE_IN_APP;
                    }
                    try {
                        checkIfBillingIsSupported(purchaseType, service);

                        PurchaseGetter getter = new PurchaseGetter(mContext);
                        Purchases purchases = getter.get(service, type);

                        postGetInventorySuccess(purchases, handler);
                    } catch (BillingException e) {
                        postOnError(e, handler);
                    }
                }

                @Override
                public void onError(BillingException e) {
                    postBindServiceError(e, handler);
                }
            });
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
     * @return
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        synchronized (this) {
            PurchaseFlowLauncher launcher = mPurchaseFlows.get(requestCode);
            if (launcher == null) {
                return false;
            }
            try {
                Checker.isMainThread();
                Purchase purchase = launcher.handleResult(requestCode, resultCode, data);

                postPurchaseSuccess(purchase);
            } catch (BillingException e) {
                postPurchaseError(e);
            } finally {
                mPurchaseFlows.delete(requestCode);
            }
            return true;
        }
    }

    /**
     * Cancel the all purchase flows
     * It will clear the pending purchase flows and ignore any event until a new request
     * <p>
     * If you don't need the BillingProcessor any more,
     * call directly {@link BillingProcessor#release()} instead
     * <p>
     * By canceling it will not cancel the purchase process
     * since the purchase process is not controlled by the app.
     */
    public void cancel() {
        synchronized (this) {
            if (mIsReleased) {
                return;
            }
            mPurchaseFlows.clear();

            if (mMainHandler != null) {
                mMainHandler.removeCallbacksAndMessages(null);
            }
            if (mWorkHandler != null) {
                mWorkHandler.removeCallbacksAndMessages(null);
            }
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
            mIsReleased = true;
            mPurchaseFlows.clear();

            if (mMainHandler != null) {
                mMainHandler.removeCallbacksAndMessages(null);
            }
            if (mWorkHandler != null) {
                mWorkHandler.removeCallbacksAndMessages(null);
                mWorkHandler.getLooper().quit();
            }
        }
    }

    protected void checkIfBillingIsSupported(PurchaseType purchaseType, IInAppBillingService service) throws BillingException {
        try {
            if (isSupported(purchaseType, service)) {
                return;
            }
        } catch (RemoteException e) {
            throw new BillingException(Constants.ERROR_REMOTE_EXCEPTION, e.getMessage());
        }
        if (purchaseType == PurchaseType.SUBSCRIPTION) {
            throw new BillingException(Constants.ERROR_SUBSCRIPTIONS_NOT_SUPPORTED,
                    Constants.ERROR_MSG_SUBSCRIPTIONS_NOT_SUPPORTED);
        }
        throw new BillingException(Constants.ERROR_PURCHASES_NOT_SUPPORTED,
                Constants.ERROR_MSG_PURCHASES_NOT_SUPPORTED);
    }

    /**
     * Check if the device supports InAppBilling
     *
     * @param service
     * @return true if it is supported
     */
    protected boolean isSupported(PurchaseType purchaseType, IInAppBillingService service) throws RemoteException {
        String type;

        if (purchaseType == PurchaseType.SUBSCRIPTION) {
            type = Constants.TYPE_SUBSCRIPTION;
        } else {
            type = Constants.TYPE_IN_APP;
        }

        int response = service.isBillingSupported(
                mContext.getApiVersion(),
                mContext.getContext().getPackageName(),
                type);

        if (response == Constants.BILLING_RESPONSE_RESULT_OK) {
            mLogger.d(Logger.TAG, "Subscription is AVAILABLE.");
            return true;
        }
        mLogger.w(Logger.TAG,
                String.format(Locale.US, "Subscription is NOT AVAILABLE. Response: %d", response));
        return false;
    }

    /**
     * Handler to post all actions in the library
     */
    protected Handler getWorkHandler() {
        if (mWorkHandler == null) {
            HandlerThread thread = new HandlerThread(WORK_THREAD_NAME);
            thread.start();
            mWorkHandler = new Handler(thread.getLooper());
        }
        return mWorkHandler;
    }

    /**
     * Handler to post all events in the library
     */
    protected Handler getMainHandler() {
        if (mMainHandler == null) {
            mMainHandler = new Handler(Looper.getMainLooper());
        }
        return mMainHandler;
    }

    /**
     * Get the purchase token to be used in {@link BillingProcessor#consumePurchase(String, ConsumeItemHandler)}
     */
    private String getToken(IInAppBillingService service, String itemId) throws BillingException {
        PurchaseGetter getter = new PurchaseGetter(mContext);
        Purchases purchases = getter.get(service, Constants.ITEM_TYPE_INAPP);
        Purchase purchase = purchases.getByPurchaseId(itemId);

        if (purchase == null || TextUtils.isEmpty(purchase.getToken())) {
            throw new BillingException(Constants.ERROR_PURCHASE_DATA,
                    Constants.ERROR_MSG_PURCHASE_OR_TOKEN_NULL);
        }
        return purchase.getToken();
    }

    protected ServiceBinder createServiceBinder() {
        return new ServiceBinder(mContext, mServiceIntent);
    }

    private void startPurchase(final Activity activity,
                               final int requestCode,
                               final List<String> oldItemIds,
                               final String itemId,
                               final PurchaseType purchaseType,
                               final String developerPayload,
                               final StartActivityHandler handler) {

        checkIfIsNotReleased();
        Checker.startActivityArguments(activity, itemId, purchaseType, handler);

        executeInServiceOnMainThread(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                try {
                    // Before launch the IAB activity, we check if subscriptions are supported.
                    checkIfBillingIsSupported(purchaseType, service);
                    PurchaseFlowLauncher launcher = createPurchaseFlowLauncher(purchaseType, requestCode);
                    mPurchaseFlows.append(requestCode, launcher);
                    launcher.launch(service, activity, requestCode, oldItemIds, itemId, developerPayload);

                    postActivityStartedSuccess(handler);
                } catch (BillingException e) {
                    if (e.getErrorCode() != Constants.ERROR_PURCHASE_FLOW_ALREADY_EXISTS) {
                        mPurchaseFlows.delete(requestCode);
                    }
                    postOnError(e, handler);
                }
            }

            @Override
            public void onError(BillingException e) {
                postBindServiceError(e, handler);
            }
        });
    }

    private void executeInService(final ServiceBinder.Handler serviceHandler, Handler handler) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                final ServiceBinder conn = createServiceBinder();

                conn.getServiceAsync(new ServiceBinder.Handler() {
                    @Override
                    public void onBind(IInAppBillingService service) {
                        try {
                            serviceHandler.onBind(service);
                        } finally {
                            conn.unbindService();
                        }
                    }

                    @Override
                    public void onError(BillingException e) {
                        serviceHandler.onError(e);
                    }
                });
            }
        });
    }

    private PurchaseFlowLauncher createPurchaseFlowLauncher(PurchaseType purchaseType, int requestCode) throws BillingException {
        PurchaseFlowLauncher launcher = mPurchaseFlows.get(requestCode);
        String type;

        if (launcher != null) {
            String message = String.format(Locale.US, Constants.ERROR_MSG_PURCHASE_FLOW_ALREADY_EXISTS, requestCode);
            throw new BillingException(Constants.ERROR_PURCHASE_FLOW_ALREADY_EXISTS, message);
        }

        if (purchaseType == PurchaseType.SUBSCRIPTION) {
            type = Constants.TYPE_SUBSCRIPTION;
        } else {
            type = Constants.TYPE_IN_APP;
        }
        return new PurchaseFlowLauncher(mContext, type);
    }

    private void executeInServiceOnWorkThread(final ServiceBinder.Handler serviceHandler) {
        executeInService(serviceHandler, getWorkHandler());
    }

    private void executeInServiceOnMainThread(final ServiceBinder.Handler serviceHandler) {
        executeInService(serviceHandler, getMainHandler());
    }

    private void postBindServiceError(BillingException exception, ErrorHandler handler) {
        postOnError(exception, handler);
    }

    private void postPurchaseSuccess(final Purchase purchase) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                mPurchaseHandler.call(new PurchaseResponse(purchase, null));
            }
        });
    }

    private void postPurchaseError(final BillingException e) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                mPurchaseHandler.call(new PurchaseResponse(null, e));
            }
        });
    }

    private void postGetItemDetailsSuccess(final ItemDetails itemDetails, final ItemDetailsHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onSuccess(itemDetails);
            }
        });
    }

    private void postGetPurchasesSuccess(final Purchases purchases, final PurchasesHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onSuccess(purchases);
            }
        });
    }

    private void postConsumePurchaseSuccess(final ConsumeItemHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onSuccess();
            }
        });
    }

    @Deprecated
    private void postGetInventorySuccess(final Purchases purchases, final InventoryHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onSuccess(purchases);
            }
        });
    }

    private void postActivityStartedSuccess(final StartActivityHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onSuccess();
            }
        });
    }

    private void postOnError(final BillingException e, final ErrorHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onError(e);
            }
        });
    }

    private void postEventHandler(Runnable r) {
        getMainHandler().post(r);
    }

    private void checkIfIsNotReleased() {
        if (mIsReleased) {
            throw new IllegalStateException(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
        }
    }
}