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

import java.util.ArrayList;
import java.util.List;

import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;
import jp.alessandro.android.iab.rxjava.BillingProcessorObservable;
import rx.observers.TestSubscriber;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;


/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/26.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class StartActivityTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    Activity mActivity;

    private final BillingContext mContext = DataCreator.newBillingContext(RuntimeEnvironment.application);

    private BillingProcessorObservable mProcessor;
    private Handler mWorkHandler;

    @Before
    public void setUp() {
        mProcessor = spy(new BillingProcessorObservable(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {

            }
        }));
        BillingProcessor billingProcessor = mProcessor.getBillingProcessor();
        mWorkHandler = billingProcessor.getWorkHandler();
    }

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
        startActivitySuccessError(PurchaseType.IN_APP);
    }

    @Test
    public void startActivityInAppError() throws InterruptedException, RemoteException {
        startActivitySuccessError(PurchaseType.SUBSCRIPTION);
    }

    @Test
    public void startActivityUpdateSubscriptionSuccess() throws InterruptedException, RemoteException {
        int requestCode = 1001;
        setUpStartUpdatePurchase();

        List<String> itemIds = new ArrayList<>();
        itemIds.add(Constants.TEST_PRODUCT_ID);

        TestSubscriber<Purchases> ts = new TestSubscriber<>();
        mProcessor.updateSubscription(
                mActivity,
                requestCode,
                itemIds,
                Constants.TEST_PRODUCT_ID,
                Constants.TEST_DEVELOPER_PAYLOAD)
                .subscribe(ts);
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        assertThat(ts.getOnNextEvents()).isEmpty();
        assertThat(ts.getOnErrorEvents()).isEmpty();
    }

    @Test
    public void startActivityUpdateSubscriptionError() throws InterruptedException, RemoteException {
        int requestCode = 1001;

        Bundle stubBundle = new Bundle();
        setServiceStub(stubBundle);

        List<String> itemIds = new ArrayList<>();
        itemIds.add(Constants.TEST_PRODUCT_ID);

        TestSubscriber<Purchases> ts = new TestSubscriber<>();
        mProcessor.updateSubscription(
                mActivity,
                requestCode,
                itemIds,
                Constants.TEST_PRODUCT_ID,
                Constants.TEST_DEVELOPER_PAYLOAD)
                .subscribe(ts);
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        assertThat(ts.getOnNextEvents()).isEmpty();

        BillingException e = (BillingException) ts.getOnErrorEvents().get(0);
        assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_UNEXPECTED_TYPE);
        assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL);
    }

    private void startActivitySuccess(PurchaseType type) throws InterruptedException, RemoteException {
        int requestCode = 1001;
        setUpStartPurchase();

        TestSubscriber<Purchases> ts = new TestSubscriber<>();
        mProcessor.startPurchase(
                mActivity,
                requestCode,
                Constants.TEST_PRODUCT_ID,
                type,
                Constants.TEST_DEVELOPER_PAYLOAD)
                .subscribe(ts);
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        assertThat(ts.getOnNextEvents()).isEmpty();
        assertThat(ts.getOnErrorEvents()).isEmpty();
    }

    private void startActivitySuccessError(PurchaseType type) throws InterruptedException, RemoteException {
        int requestCode = 1001;
        Bundle stubBundle = new Bundle();

        setServiceStub(stubBundle);

        TestSubscriber<Purchases> ts = new TestSubscriber<>();
        mProcessor.startPurchase(
                mActivity,
                requestCode,
                Constants.TEST_PRODUCT_ID,
                type,
                Constants.TEST_DEVELOPER_PAYLOAD)
                .subscribe(ts);
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        assertThat(ts.getOnNextEvents()).isEmpty();

        BillingException e = (BillingException) ts.getOnErrorEvents().get(0);
        assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_UNEXPECTED_TYPE);
        assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL);
    }

    private void setUpStartPurchase() {
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStubCreator.GET_BUY_INTENT, bundle);
        setServiceStub(stubBundle);
    }

    private void setUpStartUpdatePurchase() {
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        Bundle stubBundle = new Bundle();
        stubBundle.putParcelable(ServiceStubCreator.GET_BUY_INTENT_TO_REPLACE_SKUS, bundle);
        setServiceStub(stubBundle);
    }

    private void setServiceStub(final Bundle stubBundle) {
        ShadowApplication shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);
        IInAppBillingService.Stub stub = new ServiceStubCreator().create(stubBundle);
        ComponentName cn = mock(ComponentName.class);
        shadowApplication.setComponentNameAndServiceForBindService(cn, stub);
    }
}