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

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import java.util.List;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

public class BillingService implements IInAppBillingService {

    private final IInAppBillingService mService;

    public BillingService(IInAppBillingService service) {
        mService = service;
    }

    @Override
    public int isBillingSupported(int apiVersion, String packageName, String type) throws RemoteException {
        return mService.isBillingSupported(apiVersion, packageName, type);
    }

    @Override
    public Bundle getSkuDetails(int apiVersion, String packageName, String type, Bundle skusBundle) throws RemoteException {
        return mService.getSkuDetails(apiVersion, packageName, type, skusBundle);
    }

    @Override
    public Bundle getBuyIntent(int apiVersion, String packageName, String sku, String type, String developerPayload) throws RemoteException {
        return mService.getBuyIntent(apiVersion, packageName, sku, type, developerPayload);
    }

    @Override
    public Bundle getPurchases(int apiVersion, String packageName, String type, String continuationToken) throws RemoteException {
        return mService.getPurchases(apiVersion, packageName, type, continuationToken);
    }

    @Override
    public int consumePurchase(int apiVersion, String packageName, String purchaseToken) throws RemoteException {
        return mService.consumePurchase(apiVersion, packageName, purchaseToken);
    }

    @Override
    public int stub(int apiVersion, String packageName, String type) throws RemoteException {
        return mService.stub(apiVersion, packageName, type);
    }

    @Override
    public Bundle getBuyIntentToReplaceSkus(int apiVersion,
                                            String packageName,
                                            List<String> oldSkus,
                                            String newSku,
                                            String type,
                                            String developerPayload) throws RemoteException {
        return mService.getBuyIntentToReplaceSkus(apiVersion, packageName, oldSkus, newSku, type, developerPayload);
    }

    @Override
    public IBinder asBinder() {
        return mService.asBinder();
    }
}