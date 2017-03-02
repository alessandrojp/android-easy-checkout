/*
 *  Copyright (C) 2016 Alessandro Yuichi Okimoto
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *  Contact email: alessandro@alessandro.jp
 */

package jp.alessandro.android.iab;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

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
public class OnActivityResultTest {

    private BillingProcessor mProcessor;

    private final DataConverter mDataConverter = new DataConverter(Security.KEY_FACTORY_ALGORITHM, Security.SIGNATURE_ALGORITHM);
    private final BillingContext mContext = mDataConverter.newBillingContext(RuntimeEnvironment.application);
    private final ServiceStub mServiceStub = new ServiceStub();

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    Activity mActivity;

    @Test
    public void onActivityResultInAppSuccess() throws InterruptedException, RemoteException {
        onActivityResultSuccess(PurchaseType.IN_APP);
    }

    @Test
    public void onActivityResultSubscriptionSuccess() throws InterruptedException, RemoteException {
        onActivityResultSuccess(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void onActivityResultInAppSignatureVerificationFailed() throws InterruptedException, RemoteException {
        onActivityResultSignatureVerificationFailed(PurchaseType.IN_APP);
    }

    @Test
    public void onActivityResultSubscriptionSignatureVerificationFailed() throws InterruptedException, RemoteException {
        onActivityResultSignatureVerificationFailed(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void onActivityResultInAppDifferentRequestCode() throws InterruptedException, RemoteException {
        onActivityResultDifferentRequestCode(PurchaseType.IN_APP);
    }

    @Test
    public void onActivityResultSubscriptionDifferentRequestCode() throws InterruptedException, RemoteException {
        onActivityResultDifferentRequestCode(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void onActivityResultInAppDifferentThread() throws InterruptedException, RemoteException {
        onActivityResultDifferentThread(PurchaseType.IN_APP);
    }

    @Test
    public void onActivityResultSubscriptionDifferentThread() throws InterruptedException, RemoteException {
        onActivityResultDifferentThread(PurchaseType.SUBSCRIPTION);
    }

    private void onActivityResultSuccess(PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;
        final int itemIndex = 0;

        String itemId = String.format(Locale.ENGLISH, "%s_%d", DataConverter.TEST_PRODUCT_ID, itemIndex);

        setUpStartPurchase(latch, itemId, type, true);
        mProcessor.startPurchase(mActivity,
                requestCode,
                itemId,
                type,
                DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        assertThat(mProcessor.onActivityResult(requestCode, -1, mDataConverter.newOkIntent(itemIndex))).isTrue();
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException(e);
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void onActivityResultReleaseInApp() throws InterruptedException, RemoteException {
        onActivityResultWithRelease(PurchaseType.IN_APP);
    }

    @Test
    public void onActivityResultReleaseSubscription() throws InterruptedException, RemoteException {
        onActivityResultWithRelease(PurchaseType.SUBSCRIPTION);
    }

    @SuppressWarnings("checkstyle:methodlength")
    private void onActivityResultWithRelease(PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;
        final int itemIndex = 0;

        String itemId = String.format(Locale.ENGLISH, "%s_%d", DataConverter.TEST_PRODUCT_ID, itemIndex);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        PurchaseHandler handler = new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                throw new IllegalStateException();
            }
        };
        mProcessor = new BillingProcessor(mContext, handler);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_BUY_INTENT, bundle);
        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.startPurchase(mActivity,
                requestCode,
                itemId,
                type,
                DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        assertThat(mProcessor.onActivityResult(requestCode, -1, mDataConverter.newOkIntent(itemIndex))).isTrue();
                        mProcessor.release();
                        latch.countDown();
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
    public void onActivityResultCancelInApp() throws InterruptedException, RemoteException {
        onActivityResultWithCancel(PurchaseType.IN_APP);
    }

    @Test
    public void onActivityResultCancelSubscription() throws InterruptedException, RemoteException {
        onActivityResultWithCancel(PurchaseType.SUBSCRIPTION);
    }

    @SuppressWarnings("checkstyle:methodlength")
    private void onActivityResultWithCancel(PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        final int itemIndex = 0;
        String itemId = String.format(Locale.ENGLISH, "%s_%d", DataConverter.TEST_PRODUCT_ID, itemIndex);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        PurchaseHandler handler = new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                throw new IllegalStateException();
            }
        };
        mProcessor = new BillingProcessor(mContext, handler);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_BUY_INTENT, bundle);
        mServiceStub.setServiceForBinding(stubBundle);

        mProcessor.startPurchase(mActivity,
                requestCode,
                itemId,
                type,
                DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        assertThat(mProcessor.onActivityResult(requestCode, -1, mDataConverter.newOkIntent(itemIndex))).isTrue();
                        mProcessor.cancel();
                        latch.countDown();
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException(e);
                    }
                });
        latch.await(5, TimeUnit.SECONDS);
    }

    private void onActivityResultSignatureVerificationFailed(PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        setUpStartPurchase(latch, DataConverter.TEST_PRODUCT_ID, type, false);
        mProcessor.startPurchase(mActivity,
                requestCode,
                DataConverter.TEST_PRODUCT_ID,
                type,
                DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        Intent intent = mDataConverter.newIntent(0, DataConverter.TEST_JSON_RECEIPT, "");
                        assertThat(mProcessor.onActivityResult(requestCode, -1, intent)).isTrue();
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException(e);
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    private void onActivityResultDifferentRequestCode(PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        final int itemIndex = 0;
        String itemId = String.format(Locale.ENGLISH, "%s_%d", DataConverter.TEST_PRODUCT_ID, itemIndex);

        setUpStartPurchase(latch, DataConverter.TEST_PRODUCT_ID, type, false);
        mProcessor.startPurchase(mActivity,
                requestCode,
                itemId,
                type,
                DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        assertThat(mProcessor.onActivityResult(1002, -1, mDataConverter.newOkIntent(itemIndex))).isFalse();
                        latch.countDown();
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException(e);
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    private void onActivityResultDifferentThread(PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;

        setUpStartPurchase(latch, DataConverter.TEST_PRODUCT_ID, type, false);
        mProcessor.startPurchase(mActivity,
                requestCode,
                DataConverter.TEST_PRODUCT_ID,
                type,
                DataConverter.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        executeOnDifferentThread(latch, requestCode);
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException(e);
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    private void setUpStartPurchase(final CountDownLatch latch,
                                    final String itemId,
                                    final PurchaseType type,
                                    final boolean checkSuccess) throws RemoteException {

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStub.GET_BUY_INTENT, bundle);

        PurchaseHandler handler = new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                if (checkSuccess) {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getPurchase().getSku()).isEqualTo(itemId);
                } else {
                    assertThat(response.getException().getErrorCode()).isEqualTo(Constants.ERROR_VERIFICATION_FAILED);
                    assertThat(response.getException().getMessage()).isEqualTo(Constants.ERROR_MSG_VERIFICATION_FAILED);
                }
                latch.countDown();
            }
        };
        mProcessor = new BillingProcessor(mContext, handler);

        mServiceStub.setServiceForBinding(stubBundle);
    }

    private void executeOnDifferentThread(final CountDownLatch latch, final int requestCode) {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = mDataConverter.newIntent(0, DataConverter.TEST_JSON_RECEIPT, "");
                    mProcessor.onActivityResult(requestCode, -1, intent);
                } catch (IllegalStateException e) {
                    assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_METHOD_MUST_BE_CALLED_ON_UI_THREAD);
                } finally {
                    latch.countDown();
                }
            }
        });
        th.start();
    }
}