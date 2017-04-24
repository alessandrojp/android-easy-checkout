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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.alessandro.android.iab.rxjava.BillingProcessorObservable;

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
public class IsServiceAvailableTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Test
    public void isServiceAvailable() throws InterruptedException, RemoteException {
        Intent serviceIntent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        serviceIntent.setPackage(Constants.VENDING_PACKAGE);

        List<ResolveInfo> list = new ArrayList<>();
        list.add(new ResolveInfo());

        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);

        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.queryIntentServices(any(Intent.class), eq(0))).thenReturn(list);

        assertThat(BillingProcessorObservable.isServiceAvailable(context)).isTrue();
    }

    @Test
    public void isNotServiceAvailableListEmpty() throws InterruptedException, RemoteException {
        Intent serviceIntent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        serviceIntent.setPackage(Constants.VENDING_PACKAGE);

        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);

        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.queryIntentServices(any(Intent.class), eq(0))).thenReturn(Collections.<ResolveInfo>emptyList());

        assertThat(BillingProcessorObservable.isServiceAvailable(context)).isFalse();
    }

    @Test
    public void isNotServiceAvailableListNull() throws InterruptedException, RemoteException {
        Intent serviceIntent = new Intent(Constants.ACTION_BILLING_SERVICE_BIND);
        serviceIntent.setPackage(Constants.VENDING_PACKAGE);

        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);

        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.queryIntentServices(any(Intent.class), eq(0))).thenReturn(null);

        assertThat(BillingProcessorObservable.isServiceAvailable(context)).isFalse();
    }
}