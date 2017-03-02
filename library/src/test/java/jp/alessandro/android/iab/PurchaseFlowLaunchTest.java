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
import android.content.IntentSender;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import jp.alessandro.android.iab.util.DataConverter;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class PurchaseFlowLaunchTest {

    private static final String TYPE_IN_APP = "inapp";
    private static final String TYPE_SUBSCRIPTION = "subs";

    private final DataConverter mDataConverter = new DataConverter(Security.KEY_FACTORY_ALGORITHM, Security.KEY_FACTORY_ALGORITHM);
    private final BillingContext mBillingContext = mDataConverter.newBillingContext(RuntimeEnvironment.application);

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    IInAppBillingService mService;

    @Mock
    Activity mActivity;

    @Test
    public void startIntentSendIntentException() throws RemoteException, BillingException, IntentSender.SendIntentException {
        PurchaseFlowLauncher launcher = spy(new PurchaseFlowLauncher(mBillingContext, TYPE_IN_APP));
        int requestCode = 1001;
        PendingIntent pendingIntent = PendingIntent.getActivity(mBillingContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        doThrow(new IntentSender.SendIntentException()).when(mActivity).startIntentSenderForResult(
                any(IntentSender.class), anyInt(), any(Intent.class), anyInt(), anyInt(), anyInt());

        when(mService.getBuyIntent(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                "",
                TYPE_IN_APP,
                ""
        )).thenReturn(bundle);

        try {
            launcher.launch(mService, mActivity, requestCode, null, "", "");
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_SEND_INTENT_FAILED);
        } finally {
            verify(mService).getBuyIntent(
                    mBillingContext.getApiVersion(),
                    mBillingContext.getContext().getPackageName(),
                    "",
                    TYPE_IN_APP,
                    ""
            );
            verifyNoMoreInteractions(mService);
        }
    }

    @Test
    public void startIntentSenderForResultError() throws RemoteException, BillingException {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, TYPE_IN_APP);
        int requestCode = 1001;
        PendingIntent pendingIntent = PendingIntent.getActivity(mBillingContext.getContext(), 1, new Intent(), 0);

        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        pendingIntent.cancel();

        startIntentSender(bundle, launcher, requestCode, Constants.ERROR_SEND_INTENT_FAILED);
    }

    @Test
    public void startIntentSenderForResult() throws RemoteException, BillingException {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, TYPE_IN_APP);
        int requestCode = 1001;
        PendingIntent pendingIntent = PendingIntent.getActivity(mBillingContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        startIntentSender(bundle, launcher, requestCode, -1099);
    }

    private void startIntentSender(Bundle bundle,
                                   PurchaseFlowLauncher launcher,
                                   int requestCode,
                                   int errorCode) throws RemoteException {

        when(mService.getBuyIntent(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                "",
                TYPE_IN_APP,
                ""
        )).thenReturn(bundle);

        try {
            launcher.launch(mService, mActivity, requestCode, null, "", "");
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(errorCode);
        } finally {
            verify(mService).getBuyIntent(
                    mBillingContext.getApiVersion(),
                    mBillingContext.getContext().getPackageName(),
                    "",
                    TYPE_IN_APP,
                    ""
            );
            verifyNoMoreInteractions(mService);
        }
    }

    @Test
    public void bundleResponseNull() throws RemoteException {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, TYPE_IN_APP);
        int requestCode = 1001;

        try {
            launcher.launch(mService, mActivity, requestCode, null, "", "");
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_UNEXPECTED_TYPE);
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL);
        } finally {
            verify(mService).getBuyIntent(
                    mBillingContext.getApiVersion(),
                    mBillingContext.getContext().getPackageName(),
                    "",
                    TYPE_IN_APP,
                    ""
            );
            verifyNoMoreInteractions(mService);
        }
    }

    @Test
    public void remoteExceptionOnLaunch() throws RemoteException {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, TYPE_IN_APP);
        int requestCode = 1001;

        when(mService.getBuyIntent(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                "",
                TYPE_IN_APP,
                ""
        )).thenThrow(RemoteException.class);

        try {
            launcher.launch(mService, mActivity, requestCode, new ArrayList<String>(), "", "");
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_REMOTE_EXCEPTION);
        } finally {
            verify(mService).getBuyIntent(
                    mBillingContext.getApiVersion(),
                    mBillingContext.getContext().getPackageName(),
                    "",
                    TYPE_IN_APP,
                    ""
            );
            verifyNoMoreInteractions(mService);
        }
    }

    @Test
    public void pendingIntentNullUpdateSubscription() throws RemoteException {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, TYPE_SUBSCRIPTION);
        int requestCode = 1001;
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        List<String> oldSkus = new ArrayList<>();
        oldSkus.add("test");

        when(mService.getBuyIntentToReplaceSkus(
                BillingApi.VERSION_5.getValue(),
                mBillingContext.getContext().getPackageName(),
                oldSkus,
                "",
                TYPE_SUBSCRIPTION,
                ""
        )).thenReturn(bundle);

        try {
            launcher.launch(mService, mActivity, requestCode, oldSkus, "", "");
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PENDING_INTENT);
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PENDING_INTENT);
        } finally {
            verify(mService).getBuyIntentToReplaceSkus(
                    BillingApi.VERSION_5.getValue(),
                    mBillingContext.getContext().getPackageName(),
                    oldSkus,
                    "",
                    TYPE_SUBSCRIPTION,
                    ""
            );
            verifyNoMoreInteractions(mService);
        }
    }

    @Test
    public void pendingIntentNullWithLongResponseCode() throws RemoteException {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, TYPE_IN_APP);
        int requestCode = 1001;
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);

        noPendingIntent(mActivity,
                launcher,
                bundle,
                requestCode,
                Constants.ERROR_PENDING_INTENT,
                Constants.ERROR_MSG_PENDING_INTENT);
    }

    @Test
    public void pendingIntentNullWithDifferentResponseCode() throws RemoteException {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, TYPE_IN_APP);
        int requestCode = 1001;
        int responseCode = -100;
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, responseCode);

        noPendingIntent(mActivity,
                launcher,
                bundle,
                requestCode,
                responseCode,
                Constants.ERROR_MSG_UNABLE_TO_BUY);
    }

    @Test
    public void pendingIntentNullWithIntegerResponseCode()
            throws RemoteException, BillingException {

        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, TYPE_IN_APP);
        int requestCode = 1001;
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.RESPONSE_CODE, 0);

        noPendingIntent(mActivity,
                launcher,
                bundle,
                requestCode,
                Constants.ERROR_PENDING_INTENT,
                Constants.ERROR_MSG_PENDING_INTENT);
    }

    @Test
    public void noResponseCode() throws RemoteException {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, TYPE_IN_APP);
        Bundle bundle = new Bundle();
        int requestCode = 1001;

        noPendingIntent(mActivity,
                launcher,
                bundle,
                requestCode,
                Constants.ERROR_PENDING_INTENT,
                Constants.ERROR_MSG_PENDING_INTENT);
    }

    @Test
    public void stringResponseCode() throws RemoteException {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, TYPE_IN_APP);
        int requestCode = 1001;
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESPONSE_CODE, "0");

        noPendingIntent(mActivity,
                launcher,
                bundle,
                requestCode,
                Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE);
    }

    @Test
    public void lostContext() throws RemoteException {
        PurchaseFlowLauncher launcher = new PurchaseFlowLauncher(mBillingContext, TYPE_IN_APP);
        int requestCode = 1001;
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.RESPONSE_CODE, 0);

        noPendingIntent(null,
                launcher,
                bundle,
                requestCode,
                Constants.ERROR_LOST_CONTEXT,
                Constants.ERROR_MSG_LOST_CONTEXT);
    }

    private void noPendingIntent(Activity activity,
                                 PurchaseFlowLauncher launcher,
                                 Bundle bundle,
                                 int requestCode,
                                 int errorCode,
                                 String errorMessage) throws RemoteException {
        when(mService.getBuyIntent(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                "",
                TYPE_IN_APP,
                ""
        )).thenReturn(bundle);

        try {
            launcher.launch(mService, activity, requestCode, null, "", "");
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(errorCode);
            assertThat(e.getMessage()).isEqualTo(errorMessage);
        } finally {
            verify(mService).getBuyIntent(
                    mBillingContext.getApiVersion(),
                    mBillingContext.getContext().getPackageName(),
                    "",
                    TYPE_IN_APP,
                    ""
            );
            verifyNoMoreInteractions(mService);
        }
    }
}