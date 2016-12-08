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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import com.android.vending.billing.IInAppBillingService;

class ServiceBinder implements ServiceConnection {

    public interface Handler {

        void onBind(IInAppBillingService service);

        void onError();
    }

    private final Context mContext;
    private final Intent mIntent;
    private final android.os.Handler mEventHandler;

    private Handler mHandler;

    public ServiceBinder(Context context, Intent intent) {
        mContext = context.getApplicationContext();
        mIntent = intent;
        mEventHandler = new android.os.Handler();
    }

    public void unbindService() {
        synchronized (this) {
            mContext.unbindService(this);
        }
    }

    public void getServiceAsync(Handler handler) {
        synchronized (this) {
            bindService(handler);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, android.os.IBinder binder) {
        synchronized (this) {
            setBinder(binder);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        setBinder(null);
    }

    private void setBinder(android.os.IBinder binder) {
        IInAppBillingService service = IInAppBillingService.Stub.asInterface(binder);
        Handler handler = mHandler;
        mHandler = null;

        if (handler != null) {
            postBinder(service, handler);
        }
    }

    private void bindService(Handler handler) {
        if (mHandler != null) {
            return;
        }
        boolean bound = mContext.bindService(mIntent, this, Context.BIND_AUTO_CREATE);
        if (bound) {
            mHandler = handler;
        } else {
            handler.onError();
        }
    }

    private void postBinder(final IInAppBillingService service, final Handler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                if (handler != null) {
                    handler.onBind(service);
                }
            }
        });
    }

    private void postEventHandler(Runnable r) {
        mEventHandler.post(r);
    }
}