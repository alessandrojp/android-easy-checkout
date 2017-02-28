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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.PurchasesHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class GetPurchasesTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    IInAppBillingService mService;
    @Mock
    ServiceBinder mServiceBinder;
    @Mock
    Activity mActivity;

    private final BillingContext mContext = DataCreator.newBillingContext(RuntimeEnvironment.application);
    private final ShadowApplication mShadowApplication = shadowOf(RuntimeEnvironment.application);
    private final ComponentName mComponentName = mock(ComponentName.class);

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
    public void getInAppPurchases() throws InterruptedException, RemoteException {
        getPurchases(PurchaseType.IN_APP);
    }

    @Test
    public void getSubscriptionPurchases() throws InterruptedException, RemoteException {
        getPurchases(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void getInAppPurchasesError() throws InterruptedException, RemoteException {
        getPurchasesError(PurchaseType.IN_APP);
    }

    @Test
    public void getSubscriptionPurchasesError() throws InterruptedException, RemoteException {
        getPurchasesError(PurchaseType.SUBSCRIPTION);
    }

    private void getPurchasesError(PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);

        Bundle responseBundle = new Bundle();
        responseBundle.putInt(Constants.RESPONSE_CODE, 0);

        getPurchasesError(type, responseBundle, latch);

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void getPurchasesAndReleaseAndGetPurchasesAgain() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int size = 10;
        Bundle responseBundle = DataCreator.createPurchaseBundle(0, 0, size, null);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStubCreater.GET_PURCHASES, responseBundle);

        IInAppBillingService.Stub stub = new ServiceStubCreater().create(stubBundle);
        mShadowApplication.setComponentNameAndServiceForBindService(mComponentName, stub);

        mProcessor.getPurchases(PurchaseType.SUBSCRIPTION, new PurchasesHandler() {
            @Override
            public void onSuccess(Purchases purchases) {
                assertThat(purchases.getAll()).isNotNull();

                mProcessor.release();
                try {
                    mProcessor.getPurchases(PurchaseType.SUBSCRIPTION, null);
                } catch (IllegalStateException e) {
                    assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
                } finally {
                    latch.countDown();
                }
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
    public void getPurchasesWithPurchaseTypeNull() {
        try {
            mProcessor.getPurchases(null, new PurchasesHandler() {
                @Override
                public void onSuccess(Purchases purchases) {
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
    public void getPurchasesWithHandlerNull() {
        try {
            mProcessor.getPurchases(PurchaseType.IN_APP, null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    @Test
    public void getPurchasesAndCancel() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int size = 10;
        Bundle responseBundle = DataCreator.createPurchaseBundle(0, 0, size, null);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStubCreater.GET_PURCHASES, responseBundle);

        IInAppBillingService.Stub stub = new ServiceStubCreater().create(stubBundle);
        mShadowApplication.setComponentNameAndServiceForBindService(mComponentName, stub);

        mProcessor.getPurchases(PurchaseType.SUBSCRIPTION, new PurchasesHandler() {
            @Override
            public void onSuccess(Purchases purchases) {
                mProcessor.cancel();
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException(e);
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void bindServiceError() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);

        BillingContext context = DataCreator.newBillingContext(mock(Context.class));
        mProcessor = new BillingProcessor(context, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                throw new IllegalStateException();
            }
        });
        mWorkHandler = mProcessor.getWorkHandler();

        mProcessor.getPurchases(PurchaseType.IN_APP, new PurchasesHandler() {
            @Override
            public void onSuccess(Purchases purchases) {
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

    private void getPurchases(final PurchaseType type) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int size = 10;

        Bundle responseBundle = DataCreator.createPurchaseBundle(0, 0, size, null);
        setServiceStub(responseBundle);

        mProcessor.getPurchases(type, new PurchasesHandler() {
            @Override
            public void onSuccess(Purchases purchases) {
                assertThat(purchases.getSize()).isEqualTo(size);

                List<Purchase> purchaseList = purchases.getAll();
                for (Purchase p : purchaseList) {
                    assertThat(purchases.hasItemId(p.getSku())).isTrue();
                    assertThat(purchases.getByPurchaseId(p.getSku())).isNotNull();
                }
                mProcessor.release();
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

    private void getPurchasesError(final PurchaseType type,
                                   final Bundle responseBundle,
                                   final CountDownLatch latch) {

        setServiceStub(responseBundle);

        mProcessor.getPurchases(type, new PurchasesHandler() {
            @Override
            public void onSuccess(Purchases purchases) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e).isNotNull();
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_DATA);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_GET_PURCHASES_DATA_LIST);
                mProcessor.release();
                latch.countDown();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();
    }

    private void setServiceStub(final Bundle responseBundle) {
        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStubCreater.GET_PURCHASES, responseBundle);

        IInAppBillingService.Stub stub = new ServiceStubCreater().create(stubBundle);
        mShadowApplication.setComponentNameAndServiceForBindService(mComponentName, stub);
    }
}