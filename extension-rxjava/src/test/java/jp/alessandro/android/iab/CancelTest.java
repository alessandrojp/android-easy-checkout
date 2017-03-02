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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Locale;

import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.response.PurchaseResponse;
import jp.alessandro.android.iab.rxjava.BillingProcessorObservable;
import jp.alessandro.android.iab.util.ServiceStub;
import jp.alessandro.android.iab.util.DataConverter;
import rx.observers.TestSubscriber;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;


/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/26.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class CancelTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private final DataConverter mDataConverter = new DataConverter(Security.KEY_FACTORY_ALGORITHM, Security.SIGNATURE_ALGORITHM);
    private final BillingContext mContext = mDataConverter.newBillingContext(RuntimeEnvironment.application);
    private final ServiceStub mServiceStub = new ServiceStub();

    private BillingProcessorObservable mProcessor;
    private Handler mWorkHandler;

    @Before
    public void setUp() {
        mProcessor = new BillingProcessorObservable(mContext, new PurchaseHandler() {
            @Override
            public void call(PurchaseResponse response) {

            }
        });
        BillingProcessor billingProcessor = mProcessor.getBillingProcessor();
        mWorkHandler = billingProcessor.getWorkHandler();
    }

    @Test
    public void cancel() throws InterruptedException, RemoteException {
        int responseCode = 0;
        Bundle responseBundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, 10, null);
        Bundle stubBundle = new Bundle();
        stubBundle.putInt(ServiceStub.CONSUME_PURCHASE, responseCode);
        stubBundle.putParcelable(ServiceStub.GET_PURCHASES, responseBundle);

        mServiceStub.setServiceForBinding(stubBundle);

        String itemId = String.format(Locale.US, "%s_%d", DataConverter.TEST_PRODUCT_ID, 0);
        TestSubscriber<Void> ts = new TestSubscriber<>();

        mProcessor.cancel();
        mProcessor.consume(itemId).subscribe(ts);
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        assertThat(ts.getOnNextEvents()).isEmpty();
        assertThat(ts.getOnErrorEvents()).isEmpty();

        mProcessor.cancel();
        ts = new TestSubscriber<>();
        mProcessor.consume(itemId).subscribe(ts);
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        assertThat(ts.getOnNextEvents()).isEmpty();
        assertThat(ts.getOnErrorEvents()).isEmpty();
    }
}