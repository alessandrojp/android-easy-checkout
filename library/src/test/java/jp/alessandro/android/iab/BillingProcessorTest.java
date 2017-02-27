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
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class BillingProcessorTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    IInAppBillingService mService;
    @Mock
    ServiceBinder mServiceBinder;
    @Mock
    Activity mActivity;

    private final BillingContext mContext = DataCreator.newBillingContext(RuntimeEnvironment.application);

    private BillingProcessor mProcessor;

    @Before
    public void setUp() {
        mProcessor = spy(new BillingProcessor(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                assertThat(response).isNotNull();
            }
        }));
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

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        PurchaseType type = PurchaseType.IN_APP;

        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStubCreater.GET_BUY_INTENT, bundle);

        setServiceStub(stubBundle);

        mProcessor.startPurchase(
                mActivity,
                requestCode,
                Constants.TEST_PRODUCT_ID,
                type,
                Constants.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        assertThat(mProcessor.onActivityResult(requestCode, -1, DataCreator.newOkIntent())).isTrue();
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
        oldItemIds.add(Constants.TEST_PRODUCT_ID);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        setUpUpdateSubscriptionPurchase(bundle, oldItemIds);
        mProcessor.updateSubscription(
                mActivity,
                requestCode,
                oldItemIds,
                Constants.TEST_PRODUCT_ID,
                Constants.TEST_DEVELOPER_PAYLOAD,
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
        oldItemIds.add(Constants.TEST_PRODUCT_ID);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);

        setUpUpdateSubscriptionPurchase(bundle, oldItemIds);
        mProcessor.updateSubscription(
                mActivity,
                requestCode,
                oldItemIds,
                Constants.TEST_PRODUCT_ID,
                Constants.TEST_DEVELOPER_PAYLOAD,
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
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);
        PurchaseType type = PurchaseType.SUBSCRIPTION;

        startActivity(latch, bundle, requestCode, type);

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

    @Test
    public void releaseAndGetPurchases() {
        mProcessor.release();
        try {
            mProcessor.getPurchases(PurchaseType.IN_APP, null);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
        }
    }

    private void startActivity(final CountDownLatch latch,
                               Bundle bundle,
                               int requestCode,
                               PurchaseType type) throws RemoteException {
        setUpStartPurchase(bundle, type);
        mProcessor.startPurchase(
                mActivity,
                requestCode,
                Constants.TEST_PRODUCT_ID,
                type,
                Constants.TEST_DEVELOPER_PAYLOAD,
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
    }

    private void startActivityError(final CountDownLatch latch,
                                    Bundle bundle,
                                    int requestCode,
                                    PurchaseType type) throws RemoteException {
        setUpStartPurchase(bundle, type);
        mProcessor.startPurchase(
                mActivity,
                requestCode,
                Constants.TEST_PRODUCT_ID,
                type,
                Constants.TEST_DEVELOPER_PAYLOAD,
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

    private void setUpStartPurchase(Bundle bundle, PurchaseType type) throws RemoteException {
        setUpProcessor(type);
        Mockito.when(mService.getBuyIntent(
                mContext.getApiVersion(),
                mContext.getContext().getPackageName(),
                Constants.TEST_PRODUCT_ID,
                type == PurchaseType.SUBSCRIPTION ? Constants.TYPE_SUBSCRIPTION : Constants.TYPE_IN_APP,
                Constants.TEST_DEVELOPER_PAYLOAD
        )).thenReturn(bundle);
    }

    private void setUpUpdateSubscriptionPurchase(Bundle bundle, List<String> oldItems) throws RemoteException {
        setUpProcessor(PurchaseType.SUBSCRIPTION);
        Mockito.when(mService.getBuyIntentToReplaceSkus(
                BillingApi.VERSION_5.getValue(),
                mContext.getContext().getPackageName(),
                oldItems,
                Constants.TEST_PRODUCT_ID,
                Constants.TYPE_SUBSCRIPTION,
                Constants.TEST_DEVELOPER_PAYLOAD
        )).thenReturn(bundle);
    }

    private void setUpProcessor(PurchaseType purchaseType) throws RemoteException {
        doReturn(true).when(mProcessor).isSupported(purchaseType, mService);
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

    private void setServiceStub(final Bundle stubBundle) {
        ShadowApplication shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);
        IInAppBillingService.Stub stub = new ServiceStubCreater().create(stubBundle);
        ComponentName cn = mock(ComponentName.class);
        shadowApplication.setComponentNameAndServiceForBindService(cn, stub);
    }
}