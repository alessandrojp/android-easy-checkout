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

import jp.alessandro.android.iab.logger.Logger;

class ServiceBinder implements ServiceConnection {

    public interface Handler {

        void onBind(IInAppBillingService service);

        void onError(BillingException exception);
    }

    private final Context mContext;
    private final Intent mIntent;
    private final Logger mLogger;
    private final android.os.Handler mEventHandler;

    private Handler mHandler;

    public ServiceBinder(BillingContext context, Intent intent) {
        mContext = context.getContext();
        mIntent = intent;
        mLogger = context.getLogger();
        mEventHandler = new android.os.Handler();
    }

    public void unbindService() {
        setBinder(null);
        mContext.unbindService(this);
    }

    public void getServiceAsync(Handler handler) {
        bindService(handler);
    }

    @Override
    public void onServiceConnected(ComponentName name, android.os.IBinder binder) {
        setBinder(binder);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        setBinder(null);
    }

    private void setBinder(android.os.IBinder binder) {
        IInAppBillingService service = IInAppBillingService.Stub.asInterface(binder);
        Handler handler = mHandler;
        mHandler = null;

        if (handler == null) {
            return;
        }
        if (service == null) {
            BillingException e = new BillingException(
                    Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION,
                    Constants.ERROR_MSG_BIND_SERVICE_FAILED_SERVICE_NULL);

            postBinderError(e, handler);
        } else {
            postBinder(service, handler);
        }
    }

    private void bindService(Handler handler) {
        mHandler = handler;
        try {
            boolean bound = mContext.bindService(mIntent, this, Context.BIND_AUTO_CREATE);
            if (!bound) {
                mHandler = null;
                BillingException e = new BillingException(
                        Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION,
                        Constants.ERROR_MSG_BIND_SERVICE_FAILED);

                postBinderError(e, handler);
            }
        } catch (NullPointerException e) {
            onNullPointerException(e, handler);
        } catch (IllegalArgumentException e) {
            onIllegalArgumentException(e, handler);
        }
    }

    private void onNullPointerException(NullPointerException exception, Handler handler) {
        mLogger.e(Logger.TAG, exception.getMessage());

        // Meizu M3s devices may throw a NPE
        BillingException e = new BillingException(
                Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION,
                Constants.ERROR_MSG_BIND_SERVICE_FAILED_NPE);

        postBinderError(e, handler);
    }

    private void onIllegalArgumentException(IllegalArgumentException exception, Handler handler) {
        mLogger.e(Logger.TAG, exception.getMessage());

        // Some devices may throw IllegalArgumentException
        BillingException e = new BillingException(
                Constants.ERROR_BIND_SERVICE_FAILED_EXCEPTION,
                Constants.ERROR_MSG_BIND_SERVICE_FAILED_ILLEGAL_ARGUMENT);

        postBinderError(e, handler);
    }

    private void postBinder(final IInAppBillingService service, final Handler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onBind(service);
            }
        });
    }

    private void postBinderError(final BillingException exception, final Handler handler) {
        postEventHandler(new Runnable() {
            @Override
            public void run() {
                handler.onError(exception);
            }
        });
    }

    private void postEventHandler(Runnable r) {
        mEventHandler.post(r);
    }
}