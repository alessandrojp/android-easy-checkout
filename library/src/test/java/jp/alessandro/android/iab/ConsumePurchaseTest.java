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

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.alessandro.android.iab.handler.ConsumeItemHandler;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class ConsumePurchaseTest {

    private Handler mWorkHandler;
    private BillingProcessor mProcessor;

    private final BillingContext mContext = Util.newBillingContext(RuntimeEnvironment.application);

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    BillingService mService;
    @Mock
    ServiceBinder mServiceBinder;

    @Before
    public void setUp() {
        HandlerThread thread = new HandlerThread("AndroidIabThread");
        thread.start();
        // Handler to post all actions in the library
        mWorkHandler = new Handler(thread.getLooper());

        mProcessor = spy(new BillingProcessor(mContext, null));

        doReturn(mWorkHandler).when(mProcessor).getWorkHandler();
    }

    @Test
    public void consumePurchaseSuccess() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int responseCode = 0;
        PurchaseGetter getter = spy(new PurchaseGetter(mContext));
        Bundle responseBundle = Util.createPurchaseBundle(0, 0, 10, null);

        doReturn(responseCode).when(mService).consumePurchase(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);

        doReturn(true).when(mProcessor).isSupported(PurchaseType.IN_APP, mService);
        doReturn(getter).when(mProcessor).createPurchaseGetter();
        doReturn(responseBundle).when(mService).getPurchases(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.ITEM_TYPE_INAPP, null);

        doReturn(mServiceBinder).when(mProcessor).createServiceBinder();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceBinder.Handler handler = invocation.getArgument(0);
                handler.onBind(mService);
                return null;
            }
        }).when(mServiceBinder).getServiceAsync(any(ServiceBinder.Handler.class));

        mProcessor.consume(String.format(Locale.US, "%s_%d", Constants.TEST_PRODUCT_ID, 0), new ConsumeItemHandler() {
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
    @SuppressWarnings("checkstyle:methodlength")
    public void consumeError() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int responseCode = 3;

        doReturn(responseCode).when(mService).consumePurchase(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);

        doReturn(true).when(mProcessor).isSupported(PurchaseType.IN_APP, mService);
        doReturn(mServiceBinder).when(mProcessor).createServiceBinder();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceBinder.Handler handler = invocation.getArgument(0);
                handler.onBind(mService);
                return null;
            }
        }).when(mServiceBinder).getServiceAsync(any(ServiceBinder.Handler.class));
        doReturn(Constants.TEST_PURCHASE_TOKEN).when(mProcessor).getToken(mService, Constants.TEST_PRODUCT_ID);

        mProcessor.consume(Constants.TEST_PRODUCT_ID, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(responseCode);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_CONSUME);
                try {
                    verify(mService).consumePurchase(
                            mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);
                } catch (RemoteException e2) {
                }
                verifyNoMoreInteractions(mService);
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

        doReturn(responseCode).when(mService).consumePurchase(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);

        doReturn(true).when(mProcessor).isSupported(PurchaseType.IN_APP, mService);
        doReturn(mServiceBinder).when(mProcessor).createServiceBinder();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceBinder.Handler handler = invocation.getArgument(0);
                handler.onBind(mService);
                return null;
            }
        }).when(mServiceBinder).getServiceAsync(any(ServiceBinder.Handler.class));

        mProcessor.consume(Constants.TEST_PRODUCT_ID, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_UNEXPECTED_TYPE);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL);
                try {
                    verify(mService, never()).consumePurchase(
                            mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);
                } catch (RemoteException e2) {
                }
                latch.countDown();
            }
        });
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void consumePurchaseNull() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int responseCode = 3;
        PurchaseGetter getter = spy(new PurchaseGetter(mContext));
        Bundle responseBundle = Util.createPurchaseBundle(0, 0, 10, null);

        doReturn(responseCode).when(mService).consumePurchase(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);

        doReturn(true).when(mProcessor).isSupported(PurchaseType.IN_APP, mService);
        doReturn(mServiceBinder).when(mProcessor).createServiceBinder();
        doReturn(getter).when(mProcessor).createPurchaseGetter();
        doReturn(responseBundle).when(mService).getPurchases(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.ITEM_TYPE_INAPP, null);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceBinder.Handler handler = invocation.getArgument(0);
                handler.onBind(mService);
                return null;
            }
        }).when(mServiceBinder).getServiceAsync(any(ServiceBinder.Handler.class));

        mProcessor.consume(Constants.TEST_PRODUCT_ID, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_DATA);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PURCHASE_OR_TOKEN_NULL);
                try {
                    verify(mService, never()).consumePurchase(
                            mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);
                } catch (RemoteException e2) {
                }
                latch.countDown();
            }
        });
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void consumePurchaseTokenNull() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        PurchaseGetter getter = spy(new PurchaseGetter(mContext));
        Bundle responseBundle = Util.createPurchaseWithNoTokenBundle(0, 0, 10, null);

        doReturn(true).when(mProcessor).isSupported(PurchaseType.IN_APP, mService);
        doReturn(mServiceBinder).when(mProcessor).createServiceBinder();
        doReturn(getter).when(mProcessor).createPurchaseGetter();
        doReturn(responseBundle).when(mService).getPurchases(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.ITEM_TYPE_INAPP, null);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceBinder.Handler handler = invocation.getArgument(0);
                handler.onBind(mService);
                return null;
            }
        }).when(mServiceBinder).getServiceAsync(any(ServiceBinder.Handler.class));

        mProcessor.consume(String.format(Locale.US, "%s_%d", Constants.TEST_PRODUCT_ID, 0), new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_DATA);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PURCHASE_OR_TOKEN_NULL);
                try {
                    verify(mService, never()).consumePurchase(
                            mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);
                } catch (RemoteException e2) {
                }
                latch.countDown();
            }
        });
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void remoteException() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        PurchaseGetter getter = spy(new PurchaseGetter(mContext));
        Bundle responseBundle = Util.createPurchaseBundle(0, 0, 10, null);

        doThrow(RemoteException.class).when(mService).consumePurchase(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);

        doReturn(true).when(mProcessor).isSupported(PurchaseType.IN_APP, mService);
        doReturn(mServiceBinder).when(mProcessor).createServiceBinder();
        doReturn(getter).when(mProcessor).createPurchaseGetter();
        doReturn(responseBundle).when(mService).getPurchases(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.ITEM_TYPE_INAPP, null);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceBinder.Handler handler = invocation.getArgument(0);
                handler.onBind(mService);
                return null;
            }
        }).when(mServiceBinder).getServiceAsync(any(ServiceBinder.Handler.class));

        mProcessor.consume(String.format(Locale.US, "%s_%d", Constants.TEST_PRODUCT_ID, 0), new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_REMOTE_EXCEPTION);
                try {
                    verify(mService).consumePurchase(
                            mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);
                } catch (RemoteException e2) {
                }
                latch.countDown();
            }
        });
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void bindServiceError() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        doReturn(mServiceBinder).when(mProcessor).createServiceBinder();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceBinder.Handler handler = invocation.getArgument(0);
                handler.onError(new BillingException(
                        Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION,
                        Constants.ERROR_MSG_BIND_SERVICE_FAILED)
                );
                return null;
            }
        }).when(mServiceBinder).getServiceAsync(any(ServiceBinder.Handler.class));

        mProcessor.consume(null, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_BIND_SERVICE_FAILED);
                try {
                    verify(mService, never()).consumePurchase(
                            mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);
                } catch (RemoteException e2) {
                }
                latch.countDown();
            }
        });
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void billingNotSupport() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        doReturn(false).when(mProcessor).isSupported(PurchaseType.IN_APP, mService);
        doReturn(mServiceBinder).when(mProcessor).createServiceBinder();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceBinder.Handler handler = invocation.getArgument(0);
                handler.onBind(mService);
                return null;
            }
        }).when(mServiceBinder).getServiceAsync(any(ServiceBinder.Handler.class));

        mProcessor.consume(null, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASES_NOT_SUPPORTED);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PURCHASES_NOT_SUPPORTED);
                try {
                    verify(mService, never()).consumePurchase(
                            mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.TEST_PURCHASE_TOKEN);
                } catch (RemoteException e2) {
                }
                latch.countDown();
            }
        });
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }
}