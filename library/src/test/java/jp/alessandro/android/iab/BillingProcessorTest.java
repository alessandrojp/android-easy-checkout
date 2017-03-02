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
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
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
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;
import jp.alessandro.android.iab.util.DataConverter;
import jp.alessandro.android.iab.util.ServiceStub;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class BillingProcessorTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    Activity mActivity;

    private final DataConverter mDataConverter = new DataConverter(Security.KEY_FACTORY_ALGORITHM, Security.SIGNATURE_ALGORITHM);
    private final BillingContext mContext = mDataConverter.newBillingContext(RuntimeEnvironment.application);
    private final ServiceStub mServiceStub = new ServiceStub();

    private BillingProcessor mProcessor;

    @Before
    public void setUp() {
        mProcessor = new BillingProcessor(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                assertThat(response).isNotNull();
            }
        });
    }

    @Test
    public void constructorWithContextNull() {
        try {
            mProcessor = new BillingProcessor(null, new PurchaseHandler() {
                @Override
                public void call(PurchaseResponse response) {
                    new IllegalStateException();
                }
            });
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void constructorWithHandlerNull() {
        try {
            mProcessor = new BillingProcessor(mContext, null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void isServiceAvailable() {
        assertThat(mProcessor.isServiceAvailable(mContext.getContext())).isFalse();
    }

    @Test
    public void onActivityResult() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;
        final int itemIndex = 0;

        String itemId = String.format(Locale.ENGLISH, "%s_%d", DataConverter.TEST_PRODUCT_ID, itemIndex);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        PurchaseType type = PurchaseType.IN_APP;

        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_BUY_INTENT, bundle);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.startPurchase(
                mActivity,
                requestCode,
                itemId,
                type,
                DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        assertThat(mProcessor.onActivityResult(requestCode, -1, mDataConverter.newOkIntent(itemIndex))).isTrue();
                        latch.countDown();
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException(e);
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void startPurchaseInApp() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);
        PurchaseType type = PurchaseType.IN_APP;

        startActivity(latch, bundle, requestCode, type);

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void startPurchaseInAppError() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        PurchaseType type = PurchaseType.IN_APP;

        startActivityError(latch, bundle, requestCode, type);

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void updateSubscription() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        List<String> oldItemIds = new ArrayList<>();
        oldItemIds.add(DataConverter.TEST_PRODUCT_ID);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_BUY_INTENT_TO_REPLACE_SKUS, bundle);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.updateSubscription(
                mActivity,
                requestCode,
                oldItemIds,
                DataConverter.TEST_PRODUCT_ID,
                DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException();
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void updateSubscriptionError() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);

        int requestCode = 1001;
        List<String> oldItemIds = new ArrayList<>();
        oldItemIds.add(DataConverter.TEST_PRODUCT_ID);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_BUY_INTENT_TO_REPLACE_SKUS, bundle);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.updateSubscription(
                mActivity,
                requestCode,
                oldItemIds,
                DataConverter.TEST_PRODUCT_ID,
                DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        throw new IllegalStateException();
                    }

                    @Override
                    public void onError(BillingException e) {
                        assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PENDING_INTENT);
                        assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PENDING_INTENT);
                        latch.countDown();
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void updateSubscriptionListEmpty() {
        List<String> oldItemIds = new ArrayList<>();
        try {
            mProcessor.updateSubscription(null, 0, oldItemIds, null, null, null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_UPDATE_ARGUMENT_MISSING);
        }
    }

    @Test
    public void updateSubscriptionListNull() {
        try {
            mProcessor.updateSubscription(null, 0, null, null, null, null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_UPDATE_ARGUMENT_MISSING);
        }
    }

    @Test
    public void startPurchaseSubscription() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;

        List<String> oldItemIds = new ArrayList<>();
        oldItemIds.add(DataConverter.TEST_PRODUCT_ID);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_BUY_INTENT_TO_REPLACE_SKUS, bundle);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.updateSubscription(mActivity, requestCode, oldItemIds, DataConverter.TEST_PRODUCT_ID, DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException(e);
                    }
                }
        );
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void startPurchaseSubscriptionError() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        PurchaseType type = PurchaseType.SUBSCRIPTION;

        startActivityError(latch, bundle, requestCode, type);

        latch.await(15, TimeUnit.SECONDS);
    }

    private void startActivity(final CountDownLatch latch,
                               Bundle bundle,
                               int requestCode,
                               PurchaseType type) throws RemoteException {

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_BUY_INTENT, bundle);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.startPurchase(
                mActivity,
                requestCode,
                DataConverter.TEST_PRODUCT_ID,
                type,
                DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    private void startActivityError(final CountDownLatch latch,
                                    Bundle bundle,
                                    int requestCode,
                                    PurchaseType type) throws RemoteException {

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_BUY_INTENT, bundle);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.startPurchase(
                mActivity,
                requestCode,
                DataConverter.TEST_PRODUCT_ID,
                type,
                DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        throw new IllegalStateException();
                    }

                    @Override
                    public void onError(BillingException e) {
                        assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PENDING_INTENT);
                        assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PENDING_INTENT);
                        latch.countDown();
                    }
                });
    }
}