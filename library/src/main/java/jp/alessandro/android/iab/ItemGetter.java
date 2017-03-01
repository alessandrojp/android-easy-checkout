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

import jp.alessandro.android.iab.logger.Logger;

class ItemGetter {

    static final int MAX_SKU_PER_REQUEST = 20;

    private final int mApiVersion;
    private final String mPackageName;
    private final Logger mLogger;

    ItemGetter(BillingContext context) {
        mApiVersion = context.getApiVersion();
        mPackageName = context.getContext().getPackageName();
        mLogger = context.getLogger();
    }

    /**
     * In your application, you can query the item details from Google Play using the In-app Billing Version 3 API.
     * To pass a request to the In-app Billing service, first create a Bundle that contains
     * a String ArrayList of product IDs with key "ITEM_ID_LIST",
     * where each string is a product ID for an purchasable item.
     * See https://developer.android.com/google/play/billing/billing_integrate.html#QueryDetails
     *
     * @param service  in-app billing service
     * @param itemType "inapp" or "subs"
     * @param itemIds  contains the list of item ids that you want to request
     * @return
     * @throws BillingException
     */
    ItemDetails get(IInAppBillingService service,
                    String itemType,
                    ArrayList<String> itemIds) throws BillingException {

        ItemDetails itemDetails = new ItemDetails();
        List<ArrayList<String>> splitItemIdList = new ArrayList<>();

        // There reason why it splits the item ids per request
        // It's because there is a known bug on Google Api
        // https://code.google.com/archive/p/marketbilling/issues/137
        for (int i = 0; i < itemIds.size(); i += ItemGetter.MAX_SKU_PER_REQUEST) {
            int fromIndex = i;
            int toIndex = Math.min(itemIds.size(), i + ItemGetter.MAX_SKU_PER_REQUEST);

            ArrayList<String> list = new ArrayList<>(itemIds.subList(fromIndex, toIndex));
            splitItemIdList.add(list);
        }

        for (ArrayList<String> splitItemIds : splitItemIdList) {
            try {
                Bundle itemIdsBundle = createBundleItemListFromArray(splitItemIds);
                Bundle skuDetails = service.getSkuDetails(mApiVersion, mPackageName, itemType, itemIdsBundle);
                List<String> detailList = getItemsFromResponse(skuDetails);
                putAll(detailList, itemDetails);

            } catch (RemoteException e) {
                throw new BillingException(Constants.ERROR_REMOTE_EXCEPTION, e.getMessage());
            }
        }
        return itemDetails;
    }

    private Bundle createBundleItemListFromArray(ArrayList<String> itemIds) {
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(Constants.RESPONSE_ITEM_ID_LIST, itemIds);
        return bundle;
    }

    private List<String> getItemsFromResponse(Bundle bundle) throws BillingException {
        int response = ResponseExtractor.fromBundle(bundle, mLogger);
        if (response == Constants.BILLING_RESPONSE_RESULT_OK) {
            return getDetailsList(bundle);
        } else {
            throw new BillingException(response, Constants.ERROR_MSG_GET_SKU_DETAILS);
        }
    }

    private List<String> getDetailsList(Bundle bundle) throws BillingException {
        List<String> detailsList = bundle.getStringArrayList(Constants.RESPONSE_DETAILS_LIST);
        if (detailsList == null) {
            throw new BillingException(
                    Constants.ERROR_UNEXPECTED_TYPE, Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL);
        }
        return detailsList;
    }

    private void putAll(List<String> detailsList, ItemDetails itemDetails) throws BillingException {
        for (String response : detailsList) {
            try {
                Item product = Item.parseJson(response);
                itemDetails.put(product);
            } catch (JSONException e) {
                mLogger.e(Logger.TAG, e.getMessage(), e);
                throw new BillingException(
                        Constants.ERROR_BAD_RESPONSE, Constants.ERROR_MSG_BAD_RESPONSE);
            }
        }
    }
}