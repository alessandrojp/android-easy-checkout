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

import android.os.Bundle;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

class ItemGetter {

    private final int mApiVersion;
    private final String mPackageName;

    ItemGetter(BillingContext context) {
        mApiVersion = context.getApiVersion();
        mPackageName = context.getContext().getPackageName();
    }

    /**
     * In your application, you can query the item details from Google Play using the In-app Billing Version 3 API.
     * To pass a request to the In-app Billing service, first create a Bundle that contains
     * a String ArrayList of product IDs with key "ITEM_ID_LIST",
     * where each string is a product ID for an purchasable item.
     * See https://developer.android.com/google/play/billing/billing_integrate.html#QueryDetails
     *
     * @param service    in-app billing service
     * @param itemType   "inapp" or "subs"
     * @param itemIdList a list of the item ids that you want to request
     * @return
     * @throws BillingException
     */
    public ItemList get(IInAppBillingService service, String itemType,
                        ArrayList<String> itemIdList) throws BillingException {
        ItemList itemList = new ItemList();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(Constants.RESPONSE_ITEM_ID_LIST, itemIdList);
        try {
            Bundle skuDetails = service.getSkuDetails(mApiVersion, mPackageName, itemType, bundle);

            checkResponse(skuDetails, itemList);
        } catch (RemoteException e) {
            throw new BillingException(Constants.ERROR_REMOTE_EXCEPTION, e.getMessage());
        }
        return itemList;
    }

    private void checkResponse(Bundle skuDetails,
                               ItemList itemList) throws BillingException {
        int response = skuDetails.getInt(Constants.RESPONSE_CODE);
        if (response == Constants.BILLING_RESPONSE_RESULT_OK) {
            checkDetailsList(skuDetails, itemList);
        } else {
            throw new BillingException(response, Constants.ERROR_MSG_GET_SKU_DETAILS);
        }
    }

    private void checkDetailsList(Bundle skuDetails,
                                  ItemList itemList) throws BillingException {
        List<String> detailsList = skuDetails.getStringArrayList(Constants.RESPONSE_DETAILS_LIST);
        if (detailsList != null) {
            parseResponse(detailsList, itemList);
        }
    }

    private void parseResponse(List<String> detailsList,
                               ItemList itemList) throws BillingException {
        for (String response : detailsList) {
            try {
                Item product = Item.parseJson(response);
                itemList.put(product);
            } catch (JSONException e) {
                throw new BillingException(Constants.ERROR_BAD_RESPONSE, e.getMessage());
            }
        }
    }
}