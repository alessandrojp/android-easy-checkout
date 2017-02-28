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
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.PurchasesHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class CancelTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    IInAppBillingService mService;
    @Mock
    ServiceBinder mServiceBinder;

    private final BillingContext mContext = DataCreator.newBillingContext(RuntimeEnvironment.application);

    private BillingProcessor mProcessor;
    private Handler mWorkHandler;

    @Before
    public void setUp() throws RemoteException {
        mProcessor = spy(new BillingProcessor(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                assertThat(response).isNotNull();
            }
        }));
        mWorkHandler = mProcessor.getWorkHandler();

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
    }

    @Test
    public void getPurchasesAndCancel() throws InterruptedException, RemoteException {
        CountDownLatch latch = new CountDownLatch(1);
        Bundle responseBundle = DataCreator.createPurchaseBundle(0, 0, 10, null);

        doReturn(responseBundle).when(mService).getPurchases(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.ITEM_TYPE_INAPP, null);

        getPurchasesAndCancel(latch, new AtomicInteger(10));
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void cancelButAlreadyReleased() throws InterruptedException, RemoteException {
        mProcessor.release();
        mProcessor.cancel();
        try {
            mProcessor.getPurchases(PurchaseType.IN_APP, null);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
        }
    }

    @Test
    public void cancelFirstBeforeDoSomething() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);

        Bundle responseBundle = DataCreator.createPurchaseBundle(0, 0, 10, null);

        doReturn(responseBundle).when(mService).getPurchases(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.ITEM_TYPE_INAPP, null);

        mProcessor.cancel();
        mProcessor.getPurchases(PurchaseType.IN_APP, new PurchasesHandler() {
            @Override
            public void onSuccess(Purchases purchases) {
                assertThat(purchases.getAll()).isNotEmpty();
                mProcessor.cancel();
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException(e);
            }
        });
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();
    }

    private void getPurchasesAndCancel(final CountDownLatch latch, final AtomicInteger times) throws InterruptedException {
        mProcessor.getPurchases(PurchaseType.IN_APP, new PurchasesHandler() {
            @Override
            public void onSuccess(Purchases purchases) {
                if (times.getAndDecrement() < 1) {
                    assertThat(purchases.getAll()).isNotEmpty();
                    latch.countDown();
                    return;
                }
                mProcessor.cancel();
                try {
                    getPurchasesAndCancel(latch, times);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException(e);
            }
        });
        Shadows.shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();
    }
}