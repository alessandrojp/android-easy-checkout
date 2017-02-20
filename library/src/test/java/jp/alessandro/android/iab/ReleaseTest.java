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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class ReleaseTest {

    private final BillingContext mContext = Util.newBillingContext(RuntimeEnvironment.application);

    private BillingProcessor mProcessor;

    @Before
    public void setUp() {
        mProcessor = new BillingProcessor(mContext, null);
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

    @Test
    @Deprecated
    public void releaseAndGetInventory() {
        mProcessor.release();
        try {
            mProcessor.getInventory(PurchaseType.IN_APP, null);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
        }
    }

    @Test
    public void releaseAndGetItemDetails() {
        mProcessor.release();
        try {
            mProcessor.getItemDetails(PurchaseType.IN_APP, null, null);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
        }
    }

    @Test
    public void releaseAndConsume() {
        mProcessor.release();
        try {
            mProcessor.consume(null, null);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
        }
    }

    @Test
    public void releaseAndStartPurchase() {
        mProcessor.release();
        try {
            mProcessor.startPurchase(null, 0, null, null, null, null);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
        }
    }

    @Test
    public void releaseAndUpdateSubscription() {
        mProcessor.release();
        try {
            List<String> oldIds = new ArrayList<>();
            oldIds.add(Constants.TEST_PRODUCT_ID);

            mProcessor.updateSubscription(null, 0, oldIds, null, null, null);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
        }
    }

    @Test
    public void releaseAndCheckOnActivityResult() {
        mProcessor.release();
        try {
            mProcessor.onActivityResult(0, 0, null);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
        }
    }
}