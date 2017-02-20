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
import android.os.HandlerThread;
import android.os.Looper;
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
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.alessandro.android.iab.handler.StartActivityHandler;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class StartActivityTest {

    private Handler mWorkHandler;
    private Handler mMainHandler;
    private BillingProcessor mProcessor;

    private final BillingContext mContext = Util.newBillingContext(RuntimeEnvironment.application);

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    BillingService mService;
    @Mock
    ServiceBinder mServiceBinder;
    @Mock
    Activity mActivity;

    @Before
    public void setUp() {
        HandlerThread thread = new HandlerThread("AndroidEasyCheckoutThread");
        thread.start();
        // Handler to post all actions in the library
        mWorkHandler = new Handler(thread.getLooper());
        // Handler to post all events in the library
        mMainHandler = new Handler(Looper.getMainLooper());
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
    @SuppressWarnings("checkstyle:methodlength")
    public void startActivityUpdateSubscriptionSuccess() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;
        final List<String> oldItemIds = new ArrayList<>();
        oldItemIds.add(Constants.TEST_PRODUCT_ID);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        when(mService.getBuyIntentToReplaceSkus(
                BillingApi.VERSION_5.getValue(),
                mContext.getContext().getPackageName(),
                oldItemIds,
                Constants.TEST_PRODUCT_ID,
                Constants.TYPE_SUBSCRIPTION,
                Constants.TEST_DEVELOPER_PAYLOAD
        )).thenReturn(bundle);

        mProcessor = spy(new BillingProcessor(mContext, null));
        setUpProcessor(bundle, PurchaseType.SUBSCRIPTION);

        doReturn(mMainHandler).when(mProcessor).getMainHandler();

        mProcessor.updateSubscription(mActivity, requestCode, oldItemIds, Constants.TEST_PRODUCT_ID, Constants.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        try {
                            verify(mService, never()).getBuyIntent(
                                    mContext.getApiVersion(),
                                    mContext.getContext().getPackageName(),
                                    Constants.TEST_PRODUCT_ID,
                                    Constants.TYPE_SUBSCRIPTION,
                                    Constants.TEST_DEVELOPER_PAYLOAD);

                            verify(mService).getBuyIntentToReplaceSkus(
                                    BillingApi.VERSION_5.getValue(),
                                    mContext.getContext().getPackageName(),
                                    oldItemIds,
                                    Constants.TEST_PRODUCT_ID,
                                    Constants.TYPE_SUBSCRIPTION,
                                    Constants.TEST_DEVELOPER_PAYLOAD);
                        } catch (RemoteException err) {
                        } finally {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException();
                    }
                }

        );
        latch.await(15, TimeUnit.SECONDS);
    }

    private void startActivitySuccess(final PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        setUpStartPurchase(type);
        mProcessor.startPurchase(mActivity, requestCode, Constants.TEST_PRODUCT_ID, type, Constants.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        try {
                            verify(mService).getBuyIntent(
                                    eq(mContext.getApiVersion()),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString());
                        } catch (RemoteException e) {
                        } finally {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException();
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    @SuppressWarnings("checkstyle:methodlength")
    private void startActivityBillingNotSupported(final PurchaseType type) throws
            InterruptedException, RemoteException {

        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        setUpStartPurchase(type);
        doReturn(false).when(mProcessor).isSupported(type, mService);

        mProcessor.startPurchase(mActivity, requestCode, Constants.TEST_PRODUCT_ID, type, Constants.TEST_DEVELOPER_PAYLOAD,
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
                        try {
                            verify(mService, never()).getBuyIntent(
                                    eq(mContext.getApiVersion()),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString());
                        } catch (RemoteException err) {
                        } finally {
                            latch.countDown();
                        }
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    private void startActivityRemoteException(final PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        setUpStartPurchase(type);

        when(mService.getBuyIntent(
                eq(mContext.getApiVersion()),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenThrow(RemoteException.class);

        mProcessor.startPurchase(mActivity, requestCode, Constants.TEST_PRODUCT_ID, type, Constants.TEST_DEVELOPER_PAYLOAD,
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

        setUpStartPurchase(type);
        mProcessor.startPurchase(mActivity, requestCode, Constants.TEST_PRODUCT_ID, type, Constants.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        callStartPurchaseSecondTime(latch, requestCode, false, type);
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException();
                    }
                }
        );
        latch.await(15, TimeUnit.SECONDS);
    }

    private void startPurchaseTwiceWithDifferentRequestCode(final PurchaseType type)
            throws InterruptedException, RemoteException {

        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        setUpStartPurchase(type);
        mProcessor.startPurchase(mActivity, requestCode, Constants.TEST_PRODUCT_ID, type, Constants.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        callStartPurchaseSecondTime(latch, 1002, true, type);
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException();
                    }
                }
        );
        latch.await(15, TimeUnit.SECONDS);
    }

    private void callStartPurchaseSecondTime(final CountDownLatch latch,
                                             final int requestCode,
                                             final boolean differentRequestCode,
                                             final PurchaseType type) {

        mProcessor.startPurchase(mActivity, requestCode, Constants.TEST_PRODUCT_ID, type, Constants.TEST_DEVELOPER_PAYLOAD,
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
                            throw new IllegalStateException();
                        }
                        assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_FLOW_ALREADY_EXISTS);
                        assertThat(e.getMessage()).isEqualTo(
                                String.format(Locale.US, Constants.ERROR_MSG_PURCHASE_FLOW_ALREADY_EXISTS, requestCode));
                        try {
                            verify(mService).getBuyIntent(
                                    eq(mContext.getApiVersion()),
                                    anyString(),
                                    anyString(),
                                    anyString(),
                                    anyString());
                        } catch (RemoteException err) {
                        } finally {
                            latch.countDown();
                        }
                    }
                }
        );
    }

    private void setUpStartPurchase(PurchaseType type) throws RemoteException {

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        mProcessor = spy(new BillingProcessor(mContext, null));

        setUpProcessor(bundle, type);

        doReturn(mWorkHandler).when(mProcessor).getWorkHandler();
    }

    private void setUpProcessor(Bundle bundle, PurchaseType type) throws RemoteException {
        doReturn(true).when(mProcessor).isSupported(type, mService);
        doReturn(mServiceBinder).when(mProcessor).createServiceBinder();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceBinder.Handler handler = invocation.getArgument(0);
                handler.onBind(mService);
                return null;
            }
        }).when(mServiceBinder).getServiceAsync(any(ServiceBinder.Handler.class));

        when(mService.getBuyIntent(
                eq(mContext.getApiVersion()),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(bundle);
    }
}