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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.alessandro.android.iab.handler.ConsumeItemHandler;
import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;
import jp.alessandro.android.iab.util.DataConverter;
import jp.alessandro.android.iab.util.ServiceStub;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class ConsumePurchaseTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private final DataConverter mDataConverter = new DataConverter(Security.KEY_FACTORY_ALGORITHM, Security.SIGNATURE_ALGORITHM);
    private final BillingContext mContext = mDataConverter.newBillingContext(RuntimeEnvironment.application);
    private final ServiceStub mServiceStub = new ServiceStub();
    private final PurchaseHandler mPurchaseHandler = new PurchaseHandler() {
        @Override
        public void call(PurchaseResponse response) {
            assertThat(response).isNotNull();
        }
    };

    private Handler mWorkHandler;
    private BillingProcessor mProcessor;

    @Before
    public void setUp() {
        mProcessor = spy(new BillingProcessor(mContext, mPurchaseHandler));
        mWorkHandler = mProcessor.getWorkHandler();
    }

    @Test
    public void consumePurchaseSuccess() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int responseCode = 0;

        Bundle responseBundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, 10, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putInt(ServiceStub.CONSUME_PURCHASE, responseCode);
        stubBundle.putParcelable(ServiceStub.GET_PURCHASES, responseBundle);

        mServiceStub.setServiceForBinding(stubBundle);

        String itemId = String.format(Locale.US, "%s_%d", DataConverter.TEST_PRODUCT_ID, 0);
        mProcessor.consume(itemId, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consumePurchaseError() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int responseCode = 3;

        int size = 10;

        Bundle responseBundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, size, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putInt(ServiceStub.CONSUME_PURCHASE, responseCode);
        stubBundle.putParcelable(ServiceStub.GET_PURCHASES, responseBundle);

        mServiceStub.setServiceForBinding(stubBundle);

        String itemId = String.format(Locale.US, "%s_%d", DataConverter.TEST_PRODUCT_ID, 0);
        mProcessor.consume(itemId, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(responseCode);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_CONSUME);
                latch.countDown();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consumeWithResponseCodeNull() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int responseCode = 3;

        Bundle stubBundle = new Bundle();
        stubBundle.putInt(ServiceStub.CONSUME_PURCHASE, responseCode);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.consume(DataConverter.TEST_PRODUCT_ID, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_UNEXPECTED_TYPE);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL);
                latch.countDown();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consumePurchaseNotFound() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        Bundle responseBundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, 10, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_PURCHASES, responseBundle);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.consume(DataConverter.TEST_PRODUCT_ID, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_DATA);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PURCHASE_OR_TOKEN_NULL);
                latch.countDown();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consumePurchaseTokenNull() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        Bundle responseBundle = mDataConverter.convertToPurchaseResponseWithNoTokenBundle(0, 0, 10, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_PURCHASES, responseBundle);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.consume(String.format(Locale.US, "%s_%d", DataConverter.TEST_PRODUCT_ID, 0), new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_DATA);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PURCHASE_OR_TOKEN_NULL);
                latch.countDown();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consumePurchaseWithHandlerNull() {
        try {
            mProcessor.consume(DataConverter.TEST_PRODUCT_ID, null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void consumePurchaseWithItemIdNull() {
        try {
            mProcessor.consume(null, new ConsumeItemHandler() {
                @Override
                public void onSuccess() {
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
    public void remoteException() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        Bundle responseBundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, 10, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putBoolean(ServiceStub.THROW_REMOTE_EXCEPTION_ON_CONSUME_PURCHASE, true);
        stubBundle.putParcelable(ServiceStub.GET_PURCHASES, responseBundle);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.consume(String.format(Locale.US, "%s_%d", DataConverter.TEST_PRODUCT_ID, 0), new ConsumeItemHandler() {
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
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void bindServiceError() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        BillingContext context = mDataConverter.newBillingContext(mock(Context.class));
        mProcessor = spy(new BillingProcessor(context, mPurchaseHandler));
        mWorkHandler = mProcessor.getWorkHandler();

        mProcessor.consume(DataConverter.TEST_PRODUCT_ID, new ConsumeItemHandler() {
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
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void billingNotSupport() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        Bundle stubBundle = new Bundle();
        stubBundle.putInt(ServiceStub.IN_APP_BILLING_SUPPORTED, 1);

        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.consume(DataConverter.TEST_PRODUCT_ID, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASES_NOT_SUPPORTED);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PURCHASES_NOT_SUPPORTED);
                latch.countDown();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }
}