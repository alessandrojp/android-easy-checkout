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
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.alessandro.android.iab.handler.ItemDetailsHandler;
import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;
import jp.alessandro.android.iab.util.DataConverter;
import jp.alessandro.android.iab.util.ServiceStub;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class GetItemDetailsTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    Activity mActivity;

    private final DataConverter mDataConverter = new DataConverter(Security.KEY_FACTORY_ALGORITHM, Security.KEY_FACTORY_ALGORITHM);
    private final BillingContext mContext = mDataConverter.newBillingContext(RuntimeEnvironment.application);
    private final ServiceStub mServiceStub = new ServiceStub();

    private Handler mWorkHandler;
    private BillingProcessor mProcessor;

    @Before
    public void setUp() {
        mProcessor = new BillingProcessor(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                assertThat(response).isNotNull();
            }
        });
        mWorkHandler = mProcessor.getWorkHandler();
    }

    @Test
    public void getInAppItemDetails() throws InterruptedException, RemoteException {
        getItemDetails(PurchaseType.IN_APP);
    }

    @Test
    public void getSubscriptionItemDetails() throws InterruptedException, RemoteException {
        getItemDetails(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void getInAppItemDetailsError() throws InterruptedException, RemoteException {
        getItemDetailsError(PurchaseType.IN_APP);
    }

    @Test
    public void getSubscriptionItemDetailsError() throws InterruptedException, RemoteException {
        getItemDetailsError(PurchaseType.IN_APP);
    }

    @Test
    public void getItemDetailsWithPurchaseTypeNull() {
        ArrayList<String> itemIds = new ArrayList<>();
        try {
            mProcessor.getItemDetails(null, itemIds, new ItemDetailsHandler() {
                @Override
                public void onSuccess(ItemDetails itemDetails) {
                    throw new IllegalStateException();
                }

                @Override
                public void onError(BillingException e) {
                    throw new IllegalStateException();
                }
            });
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void getItemDetailsWithItemIdsEmpty() {
        ArrayList<String> itemIds = new ArrayList<>();
        try {
            mProcessor.getItemDetails(PurchaseType.IN_APP, itemIds, new ItemDetailsHandler() {
                @Override
                public void onSuccess(ItemDetails itemDetails) {
                    throw new IllegalStateException();
                }

                @Override
                public void onError(BillingException e) {
                    throw new IllegalStateException();
                }
            });
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void getItemDetailsWithItemIdsNull() {
        try {
            mProcessor.getItemDetails(PurchaseType.IN_APP, null, new ItemDetailsHandler() {
                @Override
                public void onSuccess(ItemDetails itemDetails) {
                    throw new IllegalStateException();
                }

                @Override
                public void onError(BillingException e) {
                    throw new IllegalStateException();
                }
            });
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void getItemDetailsWitHandlerNull() {
        ArrayList<String> itemIds = new ArrayList<>();
        itemIds.add("");
        try {
            mProcessor.getItemDetails(PurchaseType.IN_APP, itemIds, null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void bindServiceError() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        BillingContext context = mDataConverter.newBillingContext(mock(Context.class));

        ArrayList<String> itemIds = new ArrayList<>();
        itemIds.add("");

        mProcessor = new BillingProcessor(context, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                throw new IllegalStateException();
            }
        });

        mWorkHandler = mProcessor.getWorkHandler();

        mProcessor.getItemDetails(PurchaseType.IN_APP, itemIds, new ItemDetailsHandler() {
            @Override
            public void onSuccess(ItemDetails itemDetails) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_BIND_SERVICE_FAILED);
                latch.countDown();
            }
        });
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    private void getItemDetails(final PurchaseType type) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int size = 10;

        ArrayList<String> itemIds = new ArrayList<>();
        itemIds.add(DataConverter.TEST_PRODUCT_ID);

        ArrayList<String> items = mDataConverter.convertToSkuItemDetailsJsonArrayList(size);
        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);
        responseBundle.putStringArrayList(Constants.RESPONSE_DETAILS_LIST, items);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_SKU_DETAILS, responseBundle);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.getItemDetails(type, itemIds, new ItemDetailsHandler() {
            @Override
            public void onSuccess(ItemDetails itemDetails) {
                assertThat(itemDetails.getSize()).isEqualTo(size);

                List<Item> purchaseList = itemDetails.getAll();
                for (Item item : purchaseList) {
                    assertThat(itemDetails.hasItemId(item.getSku())).isTrue();
                    assertThat(itemDetails.getByItemId(item.getSku())).isNotNull();
                }
                mProcessor.release();
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException(e);
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    private void getItemDetailsError(final PurchaseType type) throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);

        ArrayList<String> itemIds = new ArrayList<>();
        itemIds.add("");

        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_SKU_DETAILS, responseBundle);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.getItemDetails(type, itemIds, new ItemDetailsHandler() {
            @Override
            public void onSuccess(ItemDetails itemDetails) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_UNEXPECTED_TYPE);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL);
                mProcessor.release();
                latch.countDown();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }
}