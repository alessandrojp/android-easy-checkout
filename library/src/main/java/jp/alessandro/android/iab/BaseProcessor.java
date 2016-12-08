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
import android.content.Intent;
import android.os.Handler;
import android.os.RemoteException;
import android.util.SparseArray;

import com.android.vending.billing.IInAppBillingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jp.alessandro.android.iab.handler.ErrorHandler;
import jp.alessandro.android.iab.handler.InventoryHandler;
import jp.alessandro.android.iab.handler.ItemDetailListHandler;
import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;
import jp.alessandro.android.iab.logger.Logger;
import jp.alessandro.android.iab.response.PurchaseResponse;

abstract class BaseProcessor {

    protected final BillingContext mContext;
    private final String mItemType;
    private final SparseArray<PurchaseFlowLauncher> mPurchaseFlows;
    private final Logger mLogger;
    private final Intent mServiceIntent;
    private final Handler mWorkHandler;
    private final Handler mMainHandler;

    private PurchaseHandler mPurchaseHandler;

    BaseProcessor(BillingContext context, String itemType,
                  PurchaseHandler purchaseHandler, Handler workHandler, Handler mainHandler) {

        mContext = context;
        mItemType = itemType;
        mPurchaseHandler = purchaseHandler;
        mWorkHandler = workHandler;
        mMainHandler = mainHandler;
        mPurchaseFlows = new SparseArray<>();
        mLogger = context.getLogger();

        mServiceIntent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        mServiceIntent.setPackage(Constants.VENDING_PACKAGE);
    }

    /**
     * Purchase a subscription
     * This will be executed from UI Thread
     *
     * @param activity         activity calling this method
     * @param requestCode
     * @param oldItemIds       a list of item ids to be updated
     * @param itemId           new subscription item id
     * @param developerPayload optional argument to be sent back with the purchase information. It helps to identify the user
     * @param handler          callback called asynchronously
     */
    public void startPurchase(final Activity activity, final int requestCode,
                              final List<String> oldItemIds, final String itemId,
                              final String developerPayload, final StartActivityHandler handler) {

        executeInServiceOnMainThread(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                try {
                    // Before launch the IAB activity, we check if subscriptions are supported.
                    checkIfBillingIsSupported(service);
                    PurchaseFlowLauncher launcher = createPurchaseFlowLauncher(requestCode);
                    mPurchaseFlows.append(requestCode, launcher);
                    launcher.launch(service, activity, requestCode, oldItemIds, itemId, developerPayload);

                    postActivityStartedSuccess(handler);
                } catch (BillingException e) {
                    postOnError(e, handler);
                }
            }

            @Override
            public void onError() {
                postBindServiceError(handler);
            }
        });
    }

    /**
     * Get a list of available SKUs details
     * See http://developer.android.com/google/play/billing/billing_integrate.html#QueryDetails
     *
     * @param itemIds list of SKU ids to be loaded
     * @param handler callback called asynchronously
     */
    public void getItemDetailList(final ArrayList<String> itemIds, final ItemDetailListHandler handler) {
        executeInServiceOnWorkThread(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                try {
                    ItemGetter getter = new ItemGetter(mContext);
                    ItemDetailList details = getter.get(service, mItemType, itemIds);
                    postListSuccess(details, handler);
                } catch (BillingException e) {
                    postOnError(e, handler);
                }
            }

            @Override
            public void onError() {
                postBindServiceError(handler);
            }
        });
    }

    /**
     * Get the information about inventory of purchases made by a user from your app
     * This method will get all the purchases even if there are more than 500
     * See http://developer.android.com/google/play/billing/billing_integrate.html#QueryPurchases
     *
     * @param handler callback called asynchronously
     */
    public void getInventory(final InventoryHandler handler) {
        executeInServiceOnWorkThread(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                try {
                    PurchaseGetter getter = new PurchaseGetter(mContext);
                    PurchaseList purchaseList = getter.get(service, mItemType);
                    postInventorySuccess(purchaseList, handler);
                } catch (BillingException e) {
                    postOnError(e, handler);
                }
            }

            @Override
            public void onError() {
                postBindServiceError(handler);
            }
        });
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
        PurchaseFlowLauncher launcher = mPurchaseFlows.get(requestCode);
        if (launcher == null) {
            return false;
        }
        try {
            Purchase purchase = launcher.handleResult(requestCode, resultCode, data);
            postPurchaseSuccess(purchase);
        } catch (BillingException e) {
            postPurchaseError(e);
        } finally {
            mPurchaseFlows.delete(requestCode);
        }
        return true;
    }

    /**
     * Release the handlers
     * By releasing it will not cancel the purchase process
     * since the purchase process is not controlled by the app.
     * Once you release it, you MUST to create a new instance
     */
    public void release() {
        mPurchaseHandler = null;
        mPurchaseFlows.clear();
    }

    protected void executeInServiceOnWorkThread(final ServiceBinder.Handler serviceHandler) {
        executeInService(serviceHandler, mWorkHandler);
    }

    protected void executeInServiceOnMainThread(final ServiceBinder.Handler serviceHandler) {
        executeInService(serviceHandler, mMainHandler);
    }

    protected void postEventHandler(Runnable r) {
        if (mMainHandler != null) {
            mMainHandler.post(r);
        }
    }

    protected void postOnError(final BillingException e, final ErrorHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                if (handler != null) {
                    handler.onError(e);
                }
            }
        });
    }

    protected void postBindServiceError(ErrorHandler handler) {
        postOnError(new BillingException(
                Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION,
                Constants.ERROR_MSG_BIND_SERVICE_FAILED), handler);
    }

    private PurchaseFlowLauncher createPurchaseFlowLauncher(int requestCode) throws BillingException {
        PurchaseFlowLauncher launcher = mPurchaseFlows.get(requestCode);
        if (launcher != null) {
            String message = String.format(Locale.US, Constants.ERROR_MSG_PURCHASE_FLOW_ALREADY_EXISTS, requestCode);
            throw new BillingException(Constants.ERROR_PURCHASE_FLOW_ALREADY_EXISTS, message);
        }
        return new PurchaseFlowLauncher(mContext, mItemType);
    }

    private void executeInService(final ServiceBinder.Handler serviceHandler, Handler handler) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                final ServiceBinder conn = new ServiceBinder(mContext.getContext(), mServiceIntent);

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
                    public void onError() {
                        serviceHandler.onError();
                    }
                });
            }
        });
    }

    private void postPurchaseSuccess(final Purchase purchase) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                if (mPurchaseHandler != null) {
                    mPurchaseHandler.call(new PurchaseResponse(purchase, null));
                }
            }
        });
    }

    private void postPurchaseError(final BillingException e) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                if (mPurchaseHandler != null) {
                    mPurchaseHandler.call(new PurchaseResponse(null, e));
                }
            }
        });
    }

    private void postListSuccess(final ItemDetailList itemDetailList, final ItemDetailListHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onSuccess(itemDetailList);
            }
        });
    }

    private void postInventorySuccess(final PurchaseList purchaseList, final InventoryHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onSuccess(purchaseList);
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

    private void checkIfBillingIsSupported(IInAppBillingService service) throws BillingException {
        if (isSupported(service)) {
            return;
        }
        if (mItemType.equals(Constants.ITEM_TYPE_INAPP)) {
            throw new BillingException(Constants.ERROR_PURCHASES_NOT_SUPPORTED,
                    Constants.ERROR_MSG_PURCHASES_NOT_SUPPORTED);
        }
        throw new BillingException(Constants.ERROR_SUBSCRIPTIONS_NOT_SUPPORTED,
                Constants.ERROR_MSG_SUBSCRIPTIONS_NOT_SUPPORTED);
    }

    private boolean isSupported(IInAppBillingService service) {
        try {
            int response = service.isBillingSupported(mContext.getApiVersion(),
                    mContext.getContext().getPackageName(), mItemType);

            if (response == Constants.BILLING_RESPONSE_RESULT_OK) {
                mLogger.d(Logger.TAG, "Subscription is AVAILABLE.");
                return true;
            }
            mLogger.w(Logger.TAG,
                    String.format(Locale.US, "Subscription is NOT AVAILABLE. Response: %d", response));
        } catch (RemoteException e) {
            mLogger.e(Logger.TAG,
                    "RemoteException while checking if the subscription is available.");
        }
        return false;
    }
}