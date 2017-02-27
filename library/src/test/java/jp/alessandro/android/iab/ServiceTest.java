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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class ServiceTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    IInAppBillingService mService;
    @Mock
    ServiceBinder mServiceBinder;
    @Mock
    Activity mActivity;

    @Before
    public void setUp() throws Exception {
        ShadowApplication shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);
        IInAppBillingService.Stub stub = new ServiceStubCreater().create(new Bundle());
        ComponentName cn = mock(ComponentName.class);
        shadowApplication.setComponentNameAndServiceForBindService(cn, stub);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedToBind() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        BillingContext context = DataCreator.newBillingContext(mock(Context.class));
        ServiceBinder conn = new ServiceBinder(context, intent);

        when(context.getContext().bindService(
                any(Intent.class),
                any(ServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE))
        ).thenReturn(false);

        conn.getServiceAsync(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_BIND_SERVICE_FAILED);
                latch.countDown();
            }
        });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void failedToBindNullPointer() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        BillingContext context = DataCreator.newBillingContext(mock(Context.class));
        ServiceBinder conn = new ServiceBinder(context, intent);

        when(context.getContext().bindService(
                any(Intent.class),
                any(ServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE))
        ).thenThrow(NullPointerException.class);

        conn.getServiceAsync(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_BIND_SERVICE_FAILED_NPE);
                latch.countDown();
            }
        });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void failedToBindIllegalArgument() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        BillingContext context = DataCreator.newBillingContext(mock(Context.class));
        ServiceBinder conn = new ServiceBinder(context, intent);

        when(context.getContext().bindService(
                any(Intent.class),
                any(ServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE))
        ).thenThrow(IllegalArgumentException.class);

        conn.getServiceAsync(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_BIND_SERVICE_FAILED_ILLEGAL_ARGUMENT);
                latch.countDown();
            }
        });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void onServiceConnectedServiceNull() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        BillingContext context = DataCreator.newBillingContext(mock(Context.class));
        ServiceBinder conn = new ServiceBinder(context, intent);

        when(context.getContext().bindService(
                any(Intent.class),
                any(ServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE))
        ).thenReturn(true);

        conn.getServiceAsync(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_BIND_SERVICE_FAILED_SERVICE_NULL);
                latch.countDown();
            }
        });
        conn.onServiceConnected(null, null);

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void onServiceConnected() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        final ServiceBinder conn = new ServiceBinder(
                DataCreator.newBillingContext(RuntimeEnvironment.application), intent);

        conn.getServiceAsync(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                assertThat(service).isNotNull();
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        });
        conn.onServiceConnected(null, new ServiceStubCreater().create(new Bundle()).asBinder());

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void onServiceDisconnected() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        final ServiceBinder conn = new ServiceBinder(
                DataCreator.newBillingContext(RuntimeEnvironment.application), intent);

        conn.getServiceAsync(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                assertThat(service).isNotNull();
                conn.onServiceDisconnected(null);
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        });
        conn.onServiceConnected(null, new ServiceStubCreater().create(new Bundle()).asBinder());

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void unbindService() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        final ServiceBinder conn = new ServiceBinder(
                DataCreator.newBillingContext(RuntimeEnvironment.application), intent);

        conn.getServiceAsync(new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                conn.unbindService();
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        });
        conn.onServiceConnected(null, new ServiceStubCreater().create(new Bundle()).asBinder());

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void callGetServiceAsyncTwice() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        final ServiceBinder conn = new ServiceBinder(
                DataCreator.newBillingContext(RuntimeEnvironment.application), intent);

        ServiceBinder.Handler handler = new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                conn.unbindService();
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        };
        conn.getServiceAsync(handler);
        conn.getServiceAsync(handler);
        conn.onServiceConnected(null, new ServiceStubCreater().create(new Bundle()).asBinder());

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void bindService() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        final ServiceBinder conn = new ServiceBinder(
                DataCreator.newBillingContext(RuntimeEnvironment.application), intent);

        ServiceBinder.Handler handler = new ServiceBinder.Handler() {
            @Override
            public void onBind(IInAppBillingService service) {
                assertThat(service).isNotNull();
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        };
        conn.getServiceAsync(handler);

        latch.await(15, TimeUnit.SECONDS);
    }
}