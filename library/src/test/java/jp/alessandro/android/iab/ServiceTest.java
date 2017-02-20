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
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    private final BillingContext mContext = Util.newBillingContext(mock(Context.class));

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    BillingService mService;
    @Mock
    ServiceBinder mServiceBinder;
    @Mock
    Activity mActivity;

    @Test
    @SuppressWarnings("unchecked")
    public void failedToBind() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        ServiceBinder conn = new ServiceBinder(mContext, intent);

        when(mContext.getContext().bindService(
                any(Intent.class),
                any(ServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE))
        ).thenReturn(false);

        conn.getServiceAsync(new ServiceBinder.Handler() {
            @Override
            public void onBind(BillingService service) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                latch.countDown();
            }
        });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void failedToBindNullPointer() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        ServiceBinder conn = new ServiceBinder(mContext, intent);

        when(mContext.getContext().bindService(
                any(Intent.class),
                any(ServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE))
        ).thenThrow(NullPointerException.class);

        conn.getServiceAsync(new ServiceBinder.Handler() {
            @Override
            public void onBind(BillingService service) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                latch.countDown();
            }
        });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void failedToBindIllegalArgument() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        ServiceBinder conn = new ServiceBinder(mContext, intent);

        when(mContext.getContext().bindService(
                any(Intent.class),
                any(ServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE))
        ).thenThrow(IllegalArgumentException.class);

        conn.getServiceAsync(new ServiceBinder.Handler() {
            @Override
            public void onBind(BillingService service) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                latch.countDown();
            }
        });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void onServiceConnectedServiceNull() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        intent.setPackage(Constants.VENDING_PACKAGE);
        ServiceBinder conn = new ServiceBinder(mContext, intent);

        when(mContext.getContext().bindService(
                any(Intent.class),
                any(ServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE))
        ).thenReturn(true);

        conn.getServiceAsync(new ServiceBinder.Handler() {
            @Override
            public void onBind(BillingService service) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                latch.countDown();
            }
        });
        conn.onServiceConnected(null, null);

        latch.await(15, TimeUnit.SECONDS);
    }
}