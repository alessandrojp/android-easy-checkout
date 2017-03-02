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
import android.content.Context;
import android.content.Intent;
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
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;
import jp.alessandro.android.iab.util.DataConverter;
import jp.alessandro.android.iab.util.ServiceStub;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class StartActivityTest {

    private BillingProcessor mProcessor;

    private final DataConverter mDataConverter = new DataConverter(Security.KEY_FACTORY_ALGORITHM, Security.KEY_FACTORY_ALGORITHM);
    private final BillingContext mContext = mDataConverter.newBillingContext(RuntimeEnvironment.application);
    private final ServiceStub mServiceStub = new ServiceStub();
    private final PurchaseHandler mPurchaseHandler = new PurchaseHandler() {
        @Override
        public void call(PurchaseResponse response) {
            assertThat(response).isNotNull();
        }
    };

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    Activity mActivity;

    @Before
    public void setUp() {
        mProcessor = new BillingProcessor(mContext, mPurchaseHandler);
    }

    @Test
    public void startActivitySubscriptionSuccess() throws InterruptedException, RemoteException {
        startActivitySuccess(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void startActivityInAppSuccess() throws InterruptedException, RemoteException {
        startActivitySuccess(PurchaseType.IN_APP);
    }

    @Test
    public void startActivityInAppBillingNotSupported() throws InterruptedException, RemoteException {
        startActivityBillingNotSupported(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void startActivitySubscriptionBillingNotSupported() throws InterruptedException, RemoteException {
        startActivityBillingNotSupported(PurchaseType.IN_APP);
    }

    @Test
    public void startActivityInAppRemoteException() throws InterruptedException, RemoteException {
        startActivityRemoteException(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void startActivitySubscriptionRemoteException() throws InterruptedException, RemoteException {
        startActivityRemoteException(PurchaseType.IN_APP);
    }

    @Test
    public void startPurchaseInAppTwiceWithSameRequestCode() throws InterruptedException, RemoteException {
        startPurchaseTwiceWithSameRequestCode(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void startPurchaseSubscriptionTwiceWithSameRequestCode() throws InterruptedException, RemoteException {
        startPurchaseTwiceWithSameRequestCode(PurchaseType.IN_APP);
    }

    @Test
    public void startPurchaseInAppTwiceWithDifferentRequestCode() throws InterruptedException, RemoteException {
        startPurchaseTwiceWithDifferentRequestCode(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void startPurchaseSubscriptionTwiceWithDifferentRequestCode() throws InterruptedException, RemoteException {
        startPurchaseTwiceWithDifferentRequestCode(PurchaseType.IN_APP);
    }

    @Test
    public void startActivityUpdateSubscriptionSuccess() throws InterruptedException, RemoteException {
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
    public void inAppIsSupportedRemoteException() throws InterruptedException, RemoteException {
        isSupportedRemoteException(PurchaseType.IN_APP);
    }

    @Test
    public void subscriptionIsSupportedRemoteException() throws InterruptedException, RemoteException {
        isSupportedRemoteException(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void startPurchaseWithActivityNull() throws RemoteException {
        final int requestCode = 1001;
        setUpStartPurchase();
        try {
            mProcessor.startPurchase(null, requestCode, DataConverter.TEST_PRODUCT_ID, PurchaseType.IN_APP, DataConverter.TEST_DEVELOPER_PAYLOAD,
                    new StartActivityHandler() {
                        @Override
                        public void onSuccess() {
                            throw new IllegalStateException();
                        }

                        @Override
                        public void onError(BillingException e) {
                            throw new IllegalStateException(e);
                        }
                    });
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void startPurchaseWithItemIdNull() throws RemoteException {
        final int requestCode = 1001;
        setUpStartPurchase();
        try {
            mProcessor.startPurchase(mActivity, requestCode, null, PurchaseType.IN_APP, DataConverter.TEST_DEVELOPER_PAYLOAD,
                    new StartActivityHandler() {
                        @Override
                        public void onSuccess() {
                            throw new IllegalStateException();
                        }

                        @Override
                        public void onError(BillingException e) {
                            throw new IllegalStateException(e);
                        }
                    });
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void startPurchaseWithPurchaseTypeNull() throws RemoteException {
        final int requestCode = 1001;
        setUpStartPurchase();
        try {
            mProcessor.startPurchase(mActivity, requestCode, DataConverter.TEST_PRODUCT_ID, null, DataConverter.TEST_DEVELOPER_PAYLOAD,
                    new StartActivityHandler() {
                        @Override
                        public void onSuccess() {
                            throw new IllegalStateException();
                        }

                        @Override
                        public void onError(BillingException e) {
                            throw new IllegalStateException(e);
                        }
                    });
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void startPurchaseWithHandlerNull() throws RemoteException {
        final int requestCode = 1001;
        setUpStartPurchase();
        try {
            mProcessor.startPurchase(
                    mActivity,
                    requestCode,
                    DataConverter.TEST_PRODUCT_ID,
                    PurchaseType.IN_APP,
                    DataConverter.TEST_DEVELOPER_PAYLOAD,
                    null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void bindServiceError() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        BillingContext context = mDataConverter.newBillingContext(mock(Context.class));

        mProcessor = new BillingProcessor(context, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                throw new IllegalStateException();
            }
        });

        Handler workHandler = mProcessor.getWorkHandler();

        mProcessor.startPurchase(mActivity, requestCode, DataConverter.TEST_PRODUCT_ID, PurchaseType.IN_APP, DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        throw new IllegalStateException();
                    }

                    @Override
                    public void onError(BillingException e) {
                        assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION);
                        assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_BIND_SERVICE_FAILED);
                        latch.countDown();
                    }
                }
        );
        Shadows.shadowOf(workHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    private void isSupportedRemoteException(PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;

        Bundle stubBundle = new Bundle();
        stubBundle.putBoolean(ServiceStub.THROW_REMOTE_EXCEPTION_ON_BILLING_SUPPORTED, true);
        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.startPurchase(mActivity, requestCode, DataConverter.TEST_PRODUCT_ID, type, DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        throw new IllegalStateException();
                    }

                    @Override
                    public void onError(BillingException e) {
                        assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_REMOTE_EXCEPTION);
                        latch.countDown();
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    private void startActivitySuccess(final PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        setUpStartPurchase();
        mProcessor.startPurchase(mActivity, requestCode, DataConverter.TEST_PRODUCT_ID, type, DataConverter.TEST_DEVELOPER_PAYLOAD,
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
        latch.await(15, TimeUnit.SECONDS);
    }

    private void startActivityBillingNotSupported(final PurchaseType type)
            throws InterruptedException, RemoteException {

        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;

        Bundle stubBundle = new Bundle();
        stubBundle.putInt(ServiceStub.IN_APP_BILLING_SUPPORTED, 1);
        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.startPurchase(mActivity, requestCode, DataConverter.TEST_PRODUCT_ID, type, DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        throw new IllegalStateException();
                    }

                    @Override
                    public void onError(BillingException e) {
                        if (type == PurchaseType.SUBSCRIPTION) {
                            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_SUBSCRIPTIONS_NOT_SUPPORTED);
                            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_SUBSCRIPTIONS_NOT_SUPPORTED);
                        } else {
                            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASES_NOT_SUPPORTED);
                            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PURCHASES_NOT_SUPPORTED);
                        }
                        latch.countDown();
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    private void startActivityRemoteException(final PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        Bundle stubBundle = new Bundle();
        stubBundle.putBoolean(ServiceStub.THROW_REMOTE_EXCEPTION_ON_GET_ACTIONS, true);
        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.startPurchase(mActivity, requestCode, DataConverter.TEST_PRODUCT_ID, type, DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        throw new IllegalStateException();
                    }

                    @Override
                    public void onError(BillingException e) {
                        assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_REMOTE_EXCEPTION);
                        latch.countDown();
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    private void startPurchaseTwiceWithSameRequestCode(final PurchaseType type)
            throws InterruptedException, RemoteException {

        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        setUpStartPurchase();
        mProcessor.startPurchase(mActivity, requestCode, DataConverter.TEST_PRODUCT_ID, type, DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        callStartPurchaseSecondTime(latch, requestCode, false, type);
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException(e);
                    }
                }
        );
        latch.await(15, TimeUnit.SECONDS);
    }

    private void startPurchaseTwiceWithDifferentRequestCode(final PurchaseType type)
            throws InterruptedException, RemoteException {

        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        setUpStartPurchase();
        mProcessor.startPurchase(mActivity, requestCode, DataConverter.TEST_PRODUCT_ID, type, DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        callStartPurchaseSecondTime(latch, 1002, true, type);
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException(e);
                    }
                }
        );
        latch.await(15, TimeUnit.SECONDS);
    }

    private void callStartPurchaseSecondTime(final CountDownLatch latch,
                                             final int requestCode,
                                             final boolean differentRequestCode,
                                             final PurchaseType type) {

        mProcessor.startPurchase(mActivity, requestCode, DataConverter.TEST_PRODUCT_ID, type, DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        if (!differentRequestCode) {
                            throw new IllegalStateException();
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onError(BillingException e) {
                        if (differentRequestCode) {
                            throw new IllegalStateException(e);
                        }
                        assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_FLOW_ALREADY_EXISTS);
                        assertThat(e.getMessage()).isEqualTo(
                                String.format(Locale.US, Constants.ERROR_MSG_PURCHASE_FLOW_ALREADY_EXISTS, requestCode));
                        latch.countDown();
                    }
                }
        );
    }

    private void setUpStartPurchase() {
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_BUY_INTENT, bundle);
        mServiceStub.setServiceForBinding(stubBundle);
    }
}