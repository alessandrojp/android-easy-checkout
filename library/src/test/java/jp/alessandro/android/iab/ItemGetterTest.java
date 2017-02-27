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
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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

    private final BillingContext mBillingContext = DataCreator.newBillingContext(RuntimeEnvironment.application);

    private ItemGetter mGetter;

    @Before
    public void setUp() {
        mGetter = new ItemGetter(mBillingContext);
    }

    @Test
    public void remoteException() throws RemoteException {
        Bundle requestBundle = new Bundle();

        Mockito.when(mService.getSkuDetails(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                requestBundle
        )).thenThrow(RemoteException.class);

        ItemDetails itemDetails = null;
        try {
            itemDetails = mGetter.get(mService, Constants.TYPE_IN_APP, requestBundle);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_REMOTE_EXCEPTION);
        } finally {
            assertThat(itemDetails).isNull();
        }
    }

    @Test
    public void getItemDetails() throws RemoteException, BillingException {
        ArrayList<String> itemIds = new ArrayList<>();
        Bundle requestBundle = new Bundle();
        requestBundle.putStringArrayList(Constants.RESPONSE_ITEM_ID_LIST, itemIds);

        int size = 10;
        ArrayList<String> items = DataCreator.createSkuItemDetailsJsonArray(size);
        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);
        responseBundle.putStringArrayList(Constants.RESPONSE_DETAILS_LIST, items);

        Mockito.when(mService.getSkuDetails(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                requestBundle
        )).thenReturn(responseBundle);

        ItemDetails itemDetails = null;
        try {
            itemDetails = mGetter.get(mService, Constants.TYPE_IN_APP, requestBundle);
        } finally {
            assertThat(itemDetails).isNotNull();
            assertThat(itemDetails.getSize()).isEqualTo(size);
            assertThat(itemDetails.getAll()).isNotNull();

            List<Item> purchaseList = itemDetails.getAll();
            for (Item p : purchaseList) {
                assertThat(itemDetails.hasItemId(p.getSku())).isTrue();
                assertThat(itemDetails.getByItemId(p.getSku())).isNotNull();
            }
        }
    }

    @Test
    public void getItemDetailsJsonBroken() throws RemoteException, BillingException {
        ArrayList<String> itemIds = new ArrayList<>();
        Bundle requestBundle = new Bundle();
        requestBundle.putStringArrayList(Constants.RESPONSE_ITEM_ID_LIST, itemIds);

        ArrayList<String> items = DataCreator.createSkuDetailsJsonBrokenArray();
        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);
        responseBundle.putStringArrayList(Constants.RESPONSE_DETAILS_LIST, items);

        getItemDetails(
                requestBundle,
                responseBundle,
                Constants.ERROR_BAD_RESPONSE,
                Constants.ERROR_MSG_BAD_RESPONSE
        );
    }

    @Test
    public void getItemDetailsWithEmptyArray() throws RemoteException {
        ArrayList<String> itemIds = new ArrayList<>();
        Bundle requestBundle = new Bundle();
        requestBundle.putStringArrayList(Constants.RESPONSE_ITEM_ID_LIST, itemIds);

        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);

        getItemDetails(
                requestBundle,
                responseBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL
        );
    }

    @Test
    public void getItemDetailsWithArrayNull() throws RemoteException {
        Bundle requestBundle = new Bundle();

        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);

        getItemDetails(
                requestBundle,
                responseBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL
        );
    }

    @Test
    public void responseBundleResponseNull() throws RemoteException {
        try {
            mGetter.get(mService, Constants.TYPE_IN_APP, null);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_UNEXPECTED_TYPE);
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL);
        } finally {
            verify(mService).getSkuDetails(
                    mBillingContext.getApiVersion(),
                    mBillingContext.getContext().getPackageName(),
                    Constants.TYPE_IN_APP,
                    null
            );
            verifyNoMoreInteractions(mService);
        }
    }

    @Test
    public void getWithLongResponseCode() throws RemoteException {
        Bundle requestBundle = new Bundle();
        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);

        getItemDetails(
                requestBundle,
                responseBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL
        );
    }

    @Test
    public void getWithDifferentResponseCode() throws RemoteException {
        Bundle requestBundle = new Bundle();
        Bundle responseBundle = new Bundle();
        responseBundle.putInt(Constants.RESPONSE_CODE, 3);

        getItemDetails(
                requestBundle,
                responseBundle,
                3,
                Constants.ERROR_MSG_GET_SKU_DETAILS
        );
    }

    @Test
    public void getWithIntegerResponseCode() throws RemoteException {
        Bundle requestBundle = new Bundle();
        Bundle responseBundle = new Bundle();
        responseBundle.putInt(Constants.RESPONSE_CODE, 0);

        getItemDetails(
                requestBundle,
                responseBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL
        );
    }

    @Test
    public void getWithNoResponseCode() throws RemoteException {
        Bundle requestBundle = new Bundle();
        Bundle responseBundle = new Bundle();

        getItemDetails(
                responseBundle,
                requestBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL
        );
    }

    @Test
    public void stringResponseCode() throws InterruptedException, RemoteException {
        Bundle requestBundle = new Bundle();
        Bundle responseBundle = new Bundle();
        responseBundle.putString(Constants.RESPONSE_CODE, "0");

        getItemDetails(
                requestBundle,
                responseBundle,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE
        );
    }

    private void getItemDetails(Bundle requestBundle,
                                Bundle responseBundle,
                                int errorCode,
                                String errorMessage) throws RemoteException {

        Mockito.when(mService.getSkuDetails(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                requestBundle
        )).thenReturn(responseBundle);

        ItemDetails itemDetails = null;
        try {
            itemDetails = mGetter.get(mService, Constants.TYPE_IN_APP, requestBundle);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(errorCode);
            assertThat(e.getMessage()).isEqualTo(errorMessage);
        } finally {
            assertThat(itemDetails).isNull();
            verify(mService).getSkuDetails(
                    mBillingContext.getApiVersion(),
                    mBillingContext.getContext().getPackageName(),
                    Constants.TYPE_IN_APP,
                    requestBundle
            );
            verifyNoMoreInteractions(mService);
        }
    }
}