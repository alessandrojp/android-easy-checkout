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

import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;
import jp.alessandro.android.iab.rxjava.BillingProcessorObservable;
import jp.alessandro.android.iab.util.DataConverter;
import jp.alessandro.android.iab.util.ServiceStub;
import rx.observers.TestSubscriber;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;


/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/26.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class OnActivityResultTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    Activity mActivity;

    private final DataConverter mDataConverter = new DataConverter(Security.KEY_FACTORY_ALGORITHM, Security.SIGNATURE_ALGORITHM);
    private final BillingContext mContext = mDataConverter.newBillingContext(RuntimeEnvironment.application);
    private final ServiceStub mServiceStub = new ServiceStub();

    private BillingProcessorObservable mProcessor;
    private Handler mWorkHandler;

    @Test
    public void startActivityInAppSuccess() throws InterruptedException, RemoteException {
        startActivitySuccess(PurchaseType.IN_APP);
    }

    @Test
    public void startActivitySubscriptionSuccess() throws InterruptedException, RemoteException {
        startActivitySuccess(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void startActivitySubscriptionError() throws InterruptedException, RemoteException {
        startActivityError(PurchaseType.IN_APP);
    }

    @Test
    public void startActivityInAppError() throws InterruptedException, RemoteException {
        startActivityError(PurchaseType.SUBSCRIPTION);
    }

    private void startActivitySuccess(PurchaseType type) throws InterruptedException, RemoteException {
        int requestCode = 1001;
        int itemIndex = 0;

        final String itemId = String.format(Locale.ENGLISH, "%s_%d", DataConverter.TEST_PRODUCT_ID, itemIndex);

        setUpStartPurchase();

        mProcessor = new BillingProcessorObservable(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                assertThat(response.isSuccess()).isTrue();
                assertThat(response.getPurchase().getSku()).isEqualTo(itemId);
            }
        });

        BillingProcessor billingProcessor = mProcessor.getBillingProcessor();
        mWorkHandler = billingProcessor.getWorkHandler();

        TestSubscriber<Purchases> ts = new TestSubscriber<>();
        mProcessor.startPurchase(
                mActivity,
                requestCode,
                itemId,
                type,
                DataConverter.TEST_DEVELOPER_PAYLOAD)
                .subscribe(ts);
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        assertThat(ts.getOnNextEvents()).isEmpty();
        assertThat(ts.getOnErrorEvents()).isEmpty();

        assertThat(mProcessor.onActivityResult(requestCode, -1, mDataConverter.newOkIntent(itemIndex))).isTrue();
    }

    private void startActivityError(PurchaseType type) throws InterruptedException, RemoteException {
        int requestCode = 1001;
        int itemIndex = 0;
        String itemId = String.format(Locale.ENGLISH, "%s_%d", DataConverter.TEST_PRODUCT_ID, itemIndex);
        setUpStartPurchase();

        mProcessor = new BillingProcessorObservable(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {
                throw new IllegalStateException();
            }
        });

        BillingProcessor billingProcessor = mProcessor.getBillingProcessor();
        mWorkHandler = billingProcessor.getWorkHandler();

        TestSubscriber<Purchases> ts = new TestSubscriber<>();
        mProcessor.startPurchase(
                mActivity,
                requestCode,
                itemId,
                type,
                DataConverter.TEST_DEVELOPER_PAYLOAD)
                .subscribe(ts);
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        assertThat(ts.getOnNextEvents()).isEmpty();
        assertThat(ts.getOnErrorEvents()).isEmpty();

        assertThat(mProcessor.onActivityResult(1002, -1, mDataConverter.newOkIntent(itemIndex))).isFalse();
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