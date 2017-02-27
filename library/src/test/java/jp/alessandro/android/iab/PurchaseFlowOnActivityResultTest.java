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
import android.content.Intent;

import com.android.vending.billing.IInAppBillingService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
public class PurchaseFlowOnActivityResultTest {

    static Intent newIntent(String data, String signature) {
        final Intent intent = new Intent();
        intent.putExtra(Constants.RESPONSE_INAPP_PURCHASE_DATA, data);
        intent.putExtra(Constants.RESPONSE_INAPP_SIGNATURE, signature);
        return intent;
    }

    @Mock
    IInAppBillingService mService;

    private final BillingContext mBillingContext = DataCreator.newBillingContext(RuntimeEnvironment.application);

    @Test
    public void purchaseJsonDataBroken() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(
                Constants.TEST_JSON_BROKEN, DataSigner.sign(Constants.TEST_JSON_BROKEN));

        intent.putExtra(Constants.RESPONSE_CODE, 0);

        checkIntent(launcher,
                requestCode,
                resultCode,
                intent,
                Constants.ERROR_BAD_RESPONSE,
                Constants.ERROR_MSG_BAD_RESPONSE);
    }

    @Test
    public void differentRequestCode() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 1;
        int resultCode = Activity.RESULT_OK;

        checkIntent(launcher,
                requestCode,
                resultCode,
                null,
                Constants.ERROR_BAD_RESPONSE,
                Constants.ERROR_MSG_RESULT_REQUEST_CODE_INVALID);
    }

    @Test
    public void unknownResultCode() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = 3;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(
                Constants.TEST_JSON_RECEIPT, DataSigner.sign(Constants.TEST_JSON_RECEIPT));

        intent.putExtra(Constants.RESPONSE_CODE, 0);

        checkIntent(launcher,
                requestCode,
                resultCode,
                intent,
                3,
                String.format(Locale.US, Constants.ERROR_MSG_RESULT_UNKNOWN, resultCode));
    }

    @Test
    public void cancelResultCode() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_CANCELED;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(null, null);
        intent.putExtra(Constants.RESPONSE_CODE, 0);

        checkIntent(launcher,
                requestCode,
                resultCode,
                intent,
                Activity.RESULT_CANCELED,
                Constants.ERROR_MSG_RESULT_CANCELED);
    }

    @Test
    public void signatureEmpty() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(Constants.TEST_JSON_RECEIPT, "");
        intent.putExtra(Constants.RESPONSE_CODE, 0);

        checkIntent(launcher,
                requestCode,
                resultCode,
                intent,
                Constants.ERROR_VERIFICATION_FAILED,
                Constants.ERROR_MSG_VERIFICATION_FAILED);
    }

    @Test
    public void signatureNull() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(Constants.TEST_JSON_RECEIPT, null);
        intent.putExtra(Constants.RESPONSE_CODE, 0);

        checkIntent(launcher,
                requestCode,
                resultCode,
                intent,
                Constants.ERROR_PURCHASE_DATA,
                Constants.ERROR_MSG_NULL_PURCHASE_DATA);
    }

    @Test
    public void purchaseDataEmpty() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent("", DataSigner.sign(Constants.TEST_JSON_RECEIPT));
        intent.putExtra(Constants.RESPONSE_CODE, 0);

        checkIntent(launcher,
                requestCode,
                resultCode,
                intent,
                Constants.ERROR_VERIFICATION_FAILED,
                Constants.ERROR_MSG_VERIFICATION_FAILED);
    }

    @Test
    public void purchaseAndSignatureEmpty() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent("", "");
        intent.putExtra(Constants.RESPONSE_CODE, 0);

        checkIntent(launcher,
                requestCode,
                resultCode,
                intent,
                Constants.ERROR_VERIFICATION_FAILED,
                Constants.ERROR_MSG_VERIFICATION_FAILED);
    }

    @Test
    public void purchaseAndSignatureNull() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(null, null);
        intent.putExtra(Constants.RESPONSE_CODE, 0);

        checkIntent(launcher,
                requestCode,
                resultCode,
                intent,
                Constants.ERROR_PURCHASE_DATA,
                Constants.ERROR_MSG_NULL_PURCHASE_DATA);
    }

    @Test
    public void purchaseDataNull() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(null, DataSigner.sign(Constants.TEST_JSON_RECEIPT));
        intent.putExtra(Constants.RESPONSE_CODE, 0);

        checkIntent(launcher,
                requestCode,
                resultCode,
                intent,
                Constants.ERROR_PURCHASE_DATA,
                Constants.ERROR_MSG_NULL_PURCHASE_DATA);
    }

    @Test
    public void intentResponseNull() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;

        checkIntent(launcher,
                requestCode,
                resultCode,
                null,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_RESULT_NULL_INTENT);
    }

    @Test
    public void intentWithLongResponseCode() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(
                Constants.TEST_JSON_RECEIPT, DataSigner.sign(Constants.TEST_JSON_RECEIPT));

        intent.putExtra(Constants.RESPONSE_CODE, 0L);

        checkIntent(launcher, requestCode, resultCode, intent, -1, null);
    }

    @Test
    public void intentWithDifferentResponseCode() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(
                Constants.TEST_JSON_RECEIPT, DataSigner.sign(Constants.TEST_JSON_RECEIPT));

        intent.putExtra(Constants.RESPONSE_CODE, -1001);

        checkIntent(launcher, requestCode, resultCode, intent, -1001, Constants.ERROR_MSG_RESULT_OK);
    }

    @Test
    public void intentWithIntegerResponseCode() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(
                Constants.TEST_JSON_RECEIPT, DataSigner.sign(Constants.TEST_JSON_RECEIPT));

        intent.putExtra(Constants.RESPONSE_CODE, 0);

        checkIntent(launcher, requestCode, resultCode, intent, -1, null);
    }

    @Test
    public void intentWithStringResponseCode() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(
                Constants.TEST_JSON_RECEIPT, DataSigner.sign(Constants.TEST_JSON_RECEIPT));

        intent.putExtra(Constants.RESPONSE_CODE, "0");

        checkIntent(launcher,
                requestCode,
                resultCode,
                intent,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE);
    }

    @Test
    public void intentWithNoResponseCode() {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, Constants.TYPE_IN_APP);
        int requestCode = 0;
        int resultCode = Activity.RESULT_OK;
        Intent intent = PurchaseFlowOnActivityResultTest.newIntent(
                Constants.TEST_JSON_RECEIPT, DataSigner.sign(Constants.TEST_JSON_RECEIPT));

        checkIntent(launcher, requestCode, resultCode, intent, -1, null);
    }

    private void checkIntent(PurchaseFlowLauncher launcher,
                             int requestCode,
                             int resultCode,
                             Intent intent,
                             int errorCode,
                             String errorMessage) {

        Purchase purchase = null;
        try {
            purchase = launcher.handleResult(requestCode, resultCode, intent);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(errorCode);
            assertThat(e.getMessage()).isEqualTo(errorMessage);
        } finally {
            if (errorCode == -1) {
                assertThat(purchase).isNotNull();
            }
        }
    }
}