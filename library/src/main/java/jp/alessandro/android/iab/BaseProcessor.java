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
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jp.alessandro.android.iab.handler.ErrorHandler;
import jp.alessandro.android.iab.handler.InventoryHandler;
import jp.alessandro.android.iab.handler.ItemListHandler;
import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.logger.Logger;

abstract class BaseProcessor {

    protected final BillingContext mContext;
    private final String mItemType;
    private final PurchaseLauncher mLauncher;
    private final Logger mLogger;
    private final Intent mServiceIntent;

    BaseProcessor(BillingContext context, String itemType) {
        mContext = context;
        mItemType = itemType;
        mLauncher = new PurchaseLauncher(mContext, mItemType);
        mLogger = context.getLogger();

        mServiceIntent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        mServiceIntent.setPackage(Constants.VENDING_PACKAGE);
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
        return list != null && (list.size() > 0);
    }

    /**
     * Get a list of available SKUs details
     * See http://developer.android.com/google/play/billing/billing_integrate.html#QueryDetails
     *
     * @param itemIds list of SKU ids to be loaded
     * @param handler callback called asynchronously
     */
    public void getSkuDetailsList(final ArrayList<String> itemIds, final ItemListHandler handler) {
        executeInService(new ServiceConnection.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                try {
                    ItemGetter getter = new ItemGetter(mContext);
                    ItemList details = getter.get(service, mItemType, itemIds);
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
        executeInService(new ServiceConnection.Handler() {
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
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @return
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        PurchaseLaunchState state = unlockPurchaseLaunchState(requestCode);
        if (state == null) {
            return false;
        }
        try {
            Purchase purchase = mLauncher.handleResult(resultCode, data);
            postPurchaseSuccess(purchase, state.getHandler());
        } catch (BillingException e) {
            postOnError(e, state.getHandler());
        }
        return true;
    }

    protected void executeInService(final ServiceConnection.Handler serviceHandler) {

        final ServiceConnection conn = new ServiceConnection(mContext.getContext(),
                mServiceIntent, mContext.getActionHandler());

        conn.getServiceAsync(new ServiceConnection.Handler() {
            @Override
            public void onBind(final IInAppBillingService service) {
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

    protected void purchase(final Activity activity, final List<String> oldItemIds, final String itemId,
                            final String developerPayload, final PurchaseHandler handler) {
        executeInService(new ServiceConnection.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                // Before launch the IAB activity, we check if subscriptions are supported.
                try {
                    checkIsSupported(service);
                    mLauncher.launch(service, activity, oldItemIds, itemId, developerPayload, handler);
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

    private PurchaseLaunchState unlockPurchaseLaunchState(int requestCode) {
        // Check if it is a consumable or subscription
        int code = mItemType.equals(Constants.ITEM_TYPE_INAPP)
                ? Constants.CONSUMABLE_REQUEST_CODE
                : Constants.SUBS_REQUEST_CODE;

        if (requestCode != code) {
            return null;
        }
        return mLauncher.tryUnlock();
    }

    protected void postEventHandler(Runnable r) {
        android.os.Handler eventHandler = mContext.getEventHandler();
        if (eventHandler != null) {
            eventHandler.post(r);
        }
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

    private void checkIsSupported(IInAppBillingService service) throws BillingException {
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

    private void postPurchaseSuccess(final Purchase purchase, final PurchaseHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onSuccess(purchase);
            }
        });
    }

    private void postListSuccess(final ItemList itemList, final ItemListHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onSuccess(itemList);
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

    protected void postBindServiceError(ErrorHandler handler) {
        postOnError(new BillingException(
                Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION,
                Constants.ERROR_MSG_BIND_SERVICE_FAILED), handler);
    }

    protected void postOnError(final BillingException e, final ErrorHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onError(e);
            }
        });
    }
}