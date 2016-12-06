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

import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.vending.billing.IInAppBillingService;

import jp.alessandro.android.iab.handler.ConsumeItemHandler;
import jp.alessandro.android.iab.handler.PurchaseHandler;

class ItemProcessor extends BaseProcessor {

    public ItemProcessor(BillingContext context, PurchaseHandler purchaseHandler,
                         Handler workHandler, Handler mainHandler) {

        super(context, Constants.ITEM_TYPE_INAPP, purchaseHandler, workHandler, mainHandler);
    }

    /**
     * Consumes previously purchased item to be purchased again
     * See http://developer.android.com/google/play/billing/billing_integrate.html#Consume
     *
     * @param itemId  consumable item id
     * @param handler callback called asynchronously
     */
    public void consume(final String itemId, final ConsumeItemHandler handler) {
        executeInServiceOnWorkThread(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                try {
                    consume(service, getToken(service, itemId));
                    postConsumeItemSuccess(handler);
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

    private String getToken(IInAppBillingService service, String itemId) throws BillingException {
        PurchaseGetter getter = new PurchaseGetter(mContext);
        PurchaseList purchaseList = getter.get(service, Constants.ITEM_TYPE_INAPP);
        Purchase purchase = purchaseList.getByPurchaseId(itemId);

        if (purchase == null || TextUtils.isEmpty(purchase.getToken())) {
            throw new BillingException(Constants.ERROR_PURCHASE_DATA,
                    Constants.ERROR_MSG_PURCHASE_OR_TOKEN_NULL);
        }
        return purchase.getToken();
    }

    private void consume(IInAppBillingService service, String purchaseToken) throws BillingException {
        try {
            int response = service.consumePurchase(mContext.getApiVersion(),
                    mContext.getContext().getPackageName(), purchaseToken);

            if (response != Constants.BILLING_RESPONSE_RESULT_OK) {
                throw new BillingException(response, Constants.ERROR_MSG_CONSUME);
            }
        } catch (RemoteException e) {
            throw new BillingException(Constants.ERROR_REMOTE_EXCEPTION, e.getMessage());
        }
    }

    private void postConsumeItemSuccess(final ConsumeItemHandler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                if (handler != null) {
                    handler.onSuccess();
                }
            }
        });
    }
}