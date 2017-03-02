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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import jp.alessandro.android.iab.util.DataConverter;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ItemGetterTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    IInAppBillingService mService;

    private final DataConverter mDataConverter = new DataConverter(Security.KEY_FACTORY_ALGORITHM, Security.KEY_FACTORY_ALGORITHM);
    private final BillingContext mBillingContext = mDataConverter.newBillingContext(RuntimeEnvironment.application);

    private ItemGetter mGetter;

    @Before
    public void setUp() {
        mGetter = new ItemGetter(mBillingContext);
    }

    @Test
    public void remoteException() throws RemoteException {
        int size = 10;
        ArrayList<String> itemIds = mDataConverter.convertToItemIdArrayList(size);

        when(mService.getSkuDetails(
                anyInt(),
                anyString(),
                anyString(),
                any(Bundle.class)
        )).thenThrow(RemoteException.class);

        ItemDetails itemDetails = null;
        try {
            itemDetails = mGetter.get(mService, Constants.TYPE_IN_APP, itemIds);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_REMOTE_EXCEPTION);
        } finally {
            assertThat(itemDetails).isNull();
            verify(mService, times(1)).getSkuDetails(
                    anyInt(),
                    anyString(),
                    anyString(),
                    any(Bundle.class)
            );
        }
    }

    @Test
    public void get20ItemDetails() throws RemoteException, BillingException {
        int size = 20;
        ArrayList<String> itemIds = mDataConverter.convertToItemIdArrayList(size);
        ArrayList<String> items = mDataConverter.convertToSkuItemDetailsJsonArrayList(size);
        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);
        responseBundle.putStringArrayList(Constants.RESPONSE_DETAILS_LIST, items);
        ItemDetails itemDetails = null;

        when(mService.getSkuDetails(
                anyInt(),
                anyString(),
                anyString(),
                any(Bundle.class)
        )).thenReturn(responseBundle);
        try {
            itemDetails = mGetter.get(mService, Constants.TYPE_IN_APP, itemIds);
        } finally {
            assertThat(itemDetails).isNotNull();
            assertThat(itemDetails.getSize()).isEqualTo(size);
            assertThat(itemDetails.getAll()).isNotNull();

            List<Item> purchaseList = itemDetails.getAll();
            for (Item p : purchaseList) {
                assertThat(itemDetails.hasItemId(p.getSku())).isTrue();
                assertThat(itemDetails.getByItemId(p.getSku())).isNotNull();
            }
            verify(mService, times(1)).getSkuDetails(
                    anyInt(),
                    anyString(),
                    anyString(),
                    any(Bundle.class)
            );
        }
    }

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void get70ItemDetails() throws RemoteException, BillingException {
        int size = 70;
        ArrayList<String> itemIds = mDataConverter.convertToItemIdArrayList(size);
        ArrayList<String> items = mDataConverter.convertToSkuItemDetailsJsonArrayList(size);
        List<Bundle> splitBundleList = new ArrayList<>();

        for (int i = 0; i < itemIds.size(); i += ItemGetter.MAX_SKU_PER_REQUEST) {
            int fromIndex = i;
            int toIndex = Math.min(itemIds.size(), i + ItemGetter.MAX_SKU_PER_REQUEST);

            Bundle bundle = new Bundle();
            ArrayList<String> list = new ArrayList<>(items.subList(fromIndex, toIndex));
            bundle.putStringArrayList(Constants.RESPONSE_DETAILS_LIST, list);
            splitBundleList.add(bundle);
        }

        when(mService.getSkuDetails(
                anyInt(),
                anyString(),
                anyString(),
                any(Bundle.class)
        )).thenReturn(splitBundleList.get(0), splitBundleList.get(1), splitBundleList.get(2), splitBundleList.get(3));

        ItemDetails itemDetails = null;
        try {
            itemDetails = mGetter.get(mService, Constants.TYPE_IN_APP, itemIds);
        } finally {
            assertThat(itemDetails).isNotNull();
            assertThat(itemDetails.getSize()).isEqualTo(size);
            assertThat(itemDetails.getAll()).isNotNull();

            List<Item> purchaseList = itemDetails.getAll();
            for (Item p : purchaseList) {
                assertThat(itemDetails.hasItemId(p.getSku())).isTrue();
                assertThat(itemDetails.getByItemId(p.getSku())).isNotNull();
            }
            verify(mService, times(4)).getSkuDetails(
                    anyInt(),
                    anyString(),
                    anyString(),
                    any(Bundle.class)
            );
        }
    }

    @Test
    public void getItemDetailsJsonBroken() throws RemoteException, BillingException {
        int size = 10;
        ArrayList<String> itemIds = mDataConverter.convertToItemIdArrayList(size);
        ArrayList<String> items = mDataConverter.convertToSkuDetailsJsonBrokenArrayList();

        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);
        responseBundle.putStringArrayList(Constants.RESPONSE_DETAILS_LIST, items);

        getItemDetails(
                itemIds,
                responseBundle,
                Constants.ERROR_BAD_RESPONSE,
                Constants.ERROR_MSG_BAD_RESPONSE
        );
    }

    @Test
    public void getItemDetailsWithEmptyArray() throws RemoteException {
        int size = 10;
        ArrayList<String> itemIds = mDataConverter.convertToItemIdArrayList(size);

        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);

        getItemDetails(
                itemIds,
                responseBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL
        );
    }

    @Test
    public void getItemDetailsWithArrayNull() throws RemoteException {
        int size = 10;
        ArrayList<String> itemIds = mDataConverter.convertToItemIdArrayList(size);

        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);

        getItemDetails(
                itemIds,
                responseBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL
        );
    }

    @Test
    public void getWithLongResponseCode() throws RemoteException {
        int size = 10;
        ArrayList<String> itemIds = mDataConverter.convertToItemIdArrayList(size);

        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);

        getItemDetails(
                itemIds,
                responseBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL
        );
    }

    @Test
    public void getWithDifferentResponseCode() throws RemoteException {
        int size = 10;
        ArrayList<String> itemIds = mDataConverter.convertToItemIdArrayList(size);

        Bundle responseBundle = new Bundle();
        responseBundle.putInt(Constants.RESPONSE_CODE, 3);

        getItemDetails(
                itemIds,
                responseBundle,
                3,
                Constants.ERROR_MSG_GET_SKU_DETAILS
        );
    }

    @Test
    public void getWithIntegerResponseCode() throws RemoteException {
        int size = 10;
        ArrayList<String> itemIds = mDataConverter.convertToItemIdArrayList(size);

        Bundle responseBundle = new Bundle();
        responseBundle.putInt(Constants.RESPONSE_CODE, 0);

        getItemDetails(
                itemIds,
                responseBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL
        );
    }

    @Test
    public void getWithNoResponseCode() throws RemoteException {
        int size = 10;
        ArrayList<String> itemIds = mDataConverter.convertToItemIdArrayList(size);

        Bundle responseBundle = new Bundle();

        getItemDetails(
                itemIds,
                responseBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL
        );
    }

    @Test
    public void stringResponseCode() throws InterruptedException, RemoteException {
        int size = 10;
        ArrayList<String> itemIds = mDataConverter.convertToItemIdArrayList(size);

        Bundle responseBundle = new Bundle();
        responseBundle.putString(Constants.RESPONSE_CODE, "0");

        getItemDetails(
                itemIds,
                responseBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE
        );
    }

    private void getItemDetails(ArrayList<String> itemIds,
                                Bundle responseBundle,
                                int errorCode,
                                String errorMessage) throws RemoteException {

        when(mService.getSkuDetails(
                anyInt(),
                anyString(),
                anyString(),
                any(Bundle.class)
        )).thenReturn(responseBundle);

        ItemDetails itemDetails = null;
        try {
            itemDetails = mGetter.get(mService, Constants.TYPE_IN_APP, itemIds);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(errorCode);
            assertThat(e.getMessage()).isEqualTo(errorMessage);
        } finally {
            assertThat(itemDetails).isNull();
            verify(mService).getSkuDetails(
                    anyInt(),
                    anyString(),
                    anyString(),
                    any(Bundle.class)
            );
        }
    }
}