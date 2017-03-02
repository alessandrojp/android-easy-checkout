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
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.PurchasesHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;
import jp.alessandro.android.iab.util.DataConverter;
import jp.alessandro.android.iab.util.ServiceStub;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class CancelTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    Activity mActivity;

    private final DataConverter mDataConverter = new DataConverter(Security.KEY_FACTORY_ALGORITHM, Security.SIGNATURE_ALGORITHM);
    private final BillingContext mContext = mDataConverter.newBillingContext(RuntimeEnvironment.application);
    private final ServiceStub mServiceStub = new ServiceStub();

    private BillingProcessor mProcessor;
    private Handler mWorkHandler;

    @Before
    public void setUp() throws RemoteException {
        mProcessor = new BillingProcessor(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                assertThat(response).isNotNull();
            }
        });
        mWorkHandler = mProcessor.getWorkHandler();
    }

    @Test
    public void cancelPattern1() {
        mProcessor = new BillingProcessor(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                assertThat(response).isNotNull();
            }
        });
        mProcessor.cancel();
        mProcessor.getWorkHandler();
        mProcessor.cancel();
        mProcessor.getMainHandler();
        mProcessor.release();
        mProcessor.cancel();
    }

    @Test
    public void cancelPattern2() {
        mProcessor = new BillingProcessor(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                assertThat(response).isNotNull();
            }
        });
        mProcessor.cancel();
        mProcessor.getMainHandler();
        mProcessor.cancel();
        mProcessor.getWorkHandler();
        mProcessor.release();
        mProcessor.cancel();
    }

    @Test
    public void cancelPattern3() {
        mProcessor = new BillingProcessor(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                assertThat(response).isNotNull();
            }
        });
        mProcessor.cancel();
        mProcessor.getMainHandler();
        mProcessor.getWorkHandler();
        mProcessor.cancel();
        mProcessor.release();
        mProcessor.cancel();
    }

    @Test
    public void cancelPattern4() {
        mProcessor = new BillingProcessor(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                assertThat(response).isNotNull();
            }
        });
        mProcessor.release();
        mProcessor.cancel();
    }

    @Test
    public void getPurchasesAndCancel() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);

        int size = 10;
        Bundle responseBundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, size, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_PURCHASES, responseBundle);

        mServiceStub.setServiceForBinding(stubBundle);

        getPurchasesAndCancel(latch, new AtomicInteger(10));
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void startActivityAndCancel() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_BUY_INTENT, bundle);
        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.startPurchase(mActivity, requestCode, DataConverter.TEST_PRODUCT_ID, PurchaseType.IN_APP, DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        mProcessor.cancel();
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

    @Test
    public void releaseFirstBeforeCancel() throws InterruptedException, RemoteException {
        mProcessor.release();
        mProcessor.cancel();
        try {
            mProcessor.getPurchases(PurchaseType.IN_APP, null);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
        }
    }

    @Test
    public void cancelFirstBeforeGetPurchases() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);

        int size = 10;
        Bundle responseBundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, size, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_PURCHASES, responseBundle);

        mServiceStub.setServiceForBinding(stubBundle);

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
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();
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
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();
    }
}