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

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.alessandro.android.iab.handler.ConsumeItemHandler;
import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class ConsumePurchaseTest {

    private Handler mWorkHandler;
    private BillingProcessor mProcessor;

    private final BillingContext mContext = DataCreator.newBillingContext(RuntimeEnvironment.application);
    private final PurchaseHandler mPurchaseHandler = new PurchaseHandler() {
        @Override
        public void call(PurchaseResponse response) {
            assertThat(response).isNotNull();
        }
    };

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    IInAppBillingService mService;
    @Mock
    ServiceBinder mServiceBinder;

    @Before
    public void setUp() {
        mProcessor = spy(new BillingProcessor(mContext, mPurchaseHandler));
        mWorkHandler = mProcessor.getWorkHandler();
    }

    @Test
    public void consumePurchaseSuccess() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int responseCode = 0;

        Bundle responseBundle = DataCreator.createPurchaseBundle(0, 0, 10, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putInt(ServiceStubCreater.CONSUME_PURCHASE, responseCode);
        stubBundle.putParcelable(ServiceStubCreater.GET_PURCHASES, responseBundle);

        setServiceStub(stubBundle);

        String itemId = String.format(Locale.US, "%s_%d", Constants.TEST_PRODUCT_ID, 0);
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
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consumePurchaseError() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int responseCode = 3;

        int size = 10;

        Bundle responseBundle = DataCreator.createPurchaseBundle(0, 0, size, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putInt(ServiceStubCreater.CONSUME_PURCHASE, responseCode);
        stubBundle.putParcelable(ServiceStubCreater.GET_PURCHASES, responseBundle);

        setServiceStub(stubBundle);

        String itemId = String.format(Locale.US, "%s_%d", Constants.TEST_PRODUCT_ID, 0);
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
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consumeWithResponseCodeNull() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int responseCode = 3;

        Bundle stubBundle = new Bundle();
        stubBundle.putInt(ServiceStubCreater.CONSUME_PURCHASE, responseCode);

        setServiceStub(stubBundle);

        mProcessor.consume(Constants.TEST_PRODUCT_ID, new ConsumeItemHandler() {
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
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consumePurchaseNotFound() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        Bundle responseBundle = DataCreator.createPurchaseBundle(0, 0, 10, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStubCreater.GET_PURCHASES, responseBundle);

        setServiceStub(stubBundle);

        mProcessor.consume(Constants.TEST_PRODUCT_ID, new ConsumeItemHandler() {
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
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consumePurchaseTokenNull() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        Bundle responseBundle = DataCreator.createPurchaseWithNoTokenBundle(0, 0, 10, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStubCreater.GET_PURCHASES, responseBundle);

        setServiceStub(stubBundle);

        mProcessor.consume(String.format(Locale.US, "%s_%d", Constants.TEST_PRODUCT_ID, 0), new ConsumeItemHandler() {
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
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consumePurchaseWithHandlerNull() {
        try {
            mProcessor.consume(Constants.TEST_PRODUCT_ID, null);
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

        Bundle stubBundle = new Bundle();
        stubBundle.putBoolean(ServiceStubCreater.THROW_REMOTE_EXCEPTION_ON_GET_ACTIONS, true);
        setServiceStub(stubBundle);

        mProcessor.consume(String.format(Locale.US, "%s_%d", Constants.TEST_PRODUCT_ID, 0), new ConsumeItemHandler() {
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
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void bindServiceError() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        BillingContext context = DataCreator.newBillingContext(mock(Context.class));
        mProcessor = spy(new BillingProcessor(context, mPurchaseHandler));
        mWorkHandler = mProcessor.getWorkHandler();

        mProcessor.consume(Constants.TEST_PRODUCT_ID, new ConsumeItemHandler() {
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
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void billingNotSupport() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        Bundle stubBundle = new Bundle();
        stubBundle.putInt(ServiceStubCreater.IN_APP_BILLING_SUPPORTED, 1);

        setServiceStub(stubBundle);

        mProcessor.consume(Constants.TEST_PRODUCT_ID, new ConsumeItemHandler() {
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
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    private void setServiceStub(final Bundle stubBundle) {
        ShadowApplication shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);
        IInAppBillingService.Stub stub = new ServiceStubCreater().create(stubBundle);
        ComponentName cn = mock(ComponentName.class);
        shadowApplication.setComponentNameAndServiceForBindService(cn, stub);
    }
}