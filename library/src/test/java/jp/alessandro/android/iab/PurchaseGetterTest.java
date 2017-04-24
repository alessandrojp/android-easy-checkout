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
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import jp.alessandro.android.iab.util.DataConverter;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PurchaseGetterTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    IInAppBillingService mService;

    private final DataConverter mDataConverter = new DataConverter(Security.KEY_FACTORY_ALGORITHM, Security.SIGNATURE_ALGORITHM);
    private final BillingContext mBillingContext = mDataConverter.newBillingContext(RuntimeEnvironment.application);

    private PurchaseGetter mGetter;

    @Before
    public void setUp() {
        mGetter = new PurchaseGetter(mBillingContext);
    }

    @Test
    public void remoteException() throws RemoteException {
        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                null
        )).thenThrow(RemoteException.class);

        Purchases purchases = null;
        try {
            purchases = mGetter.get(mService, Constants.TYPE_IN_APP);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_REMOTE_EXCEPTION);
        } finally {
            assertThat(purchases).isNull();
        }
    }

    @Test
    public void getWithDifferentSizes() throws RemoteException {
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_PURCHASE_LIST, mDataConverter.convertToPurchaseJsonArrayList(0, 5));
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_SIGNATURE_LIST, new ArrayList<String>());

        getPurchases(bundle, Constants.ERROR_PURCHASE_DATA, Constants.ERROR_MSG_GET_PURCHASES_DIFFERENT_SIZE);

        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                null
        )).thenReturn(bundle);
    }

    @Test
    public void getWithPurchasesAndSignaturesEmpty() throws RemoteException {
        Bundle bundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, 0, null);

        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                null
        )).thenReturn(bundle);

        Purchases purchases = null;
        try {
            purchases = mGetter.get(mService, Constants.TYPE_IN_APP);
        } catch (BillingException e) {

        } finally {
            assertThat(purchases).isNotNull();
            assertThat(purchases.getSize()).isZero();
        }
    }

    @Test
    public void getWithPurchasesAndSignaturesNull() throws RemoteException {
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);

        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                null
        )).thenReturn(bundle);

        Purchases purchases = null;
        try {
            purchases = mGetter.get(mService, Constants.TYPE_IN_APP);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_DATA);
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_GET_PURCHASES_DATA_LIST);
        } finally {
            assertThat(purchases).isNull();
        }
    }

    @Test
    public void getWithPurchasesNull() throws RemoteException {
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_SIGNATURE_LIST, new ArrayList<String>());

        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                null
        )).thenReturn(bundle);

        Purchases purchases = null;
        try {
            purchases = mGetter.get(mService, Constants.TYPE_IN_APP);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_DATA);
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_GET_PURCHASES_DATA_LIST);
        } finally {
            assertThat(purchases).isNull();
        }
    }

    @Test
    public void getWithSignaturesNull() throws RemoteException {
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_PURCHASE_LIST, new ArrayList<String>());

        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                null
        )).thenReturn(bundle);

        Purchases purchases = null;
        try {
            purchases = mGetter.get(mService, Constants.TYPE_IN_APP);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_DATA);
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_GET_PURCHASES_SIGNATURE_LIST);
        } finally {
            assertThat(purchases).isNull();
        }
    }

    @Test
    public void bundleResponseNull() throws RemoteException {
        try {
            mGetter.get(mService, Constants.TYPE_IN_APP);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_UNEXPECTED_TYPE);
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE_NULL);
        } finally {
            verify(mService).getPurchases(
                    mBillingContext.getApiVersion(),
                    mBillingContext.getContext().getPackageName(),
                    Constants.TYPE_IN_APP,
                    null
            );
            verifyNoMoreInteractions(mService);
        }
    }

    @Test
    public void getWithValidSignatures() throws RemoteException, BillingException {
        int size = 10;
        Bundle bundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, size, null);

        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                null
        )).thenReturn(bundle);

        Purchases purchases = null;
        try {
            purchases = mGetter.get(mService, Constants.TYPE_IN_APP);
        } finally {
            assertThat(purchases).isNotNull();
            assertThat(purchases.getSize()).isEqualTo(size);
            assertThat(purchases.getAll()).isNotNull();

            List<Purchase> purchaseList = purchases.getAll();
            for (Purchase p : purchaseList) {
                assertThat(purchases.hasItemId(p.getSku())).isTrue();
                assertThat(purchases.getByPurchaseId(p.getSku())).isNotNull();
            }
        }
    }

    @Test
    public void getWithValidSignatureUsingContinuationToken() throws RemoteException, BillingException {
        String continuationString = "continuation_token";
        Bundle bundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, 10, continuationString);
        Bundle bundle2 = mDataConverter.convertToPurchaseResponseBundle(0, 10, 10, null);

        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                null
        )).thenReturn(bundle);

        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                continuationString
        )).thenReturn(bundle2);

        Purchases purchases = null;
        try {
            purchases = mGetter.get(mService, Constants.TYPE_IN_APP);
        } finally {
            assertThat(purchases).isNotNull();
            assertThat(purchases.getSize()).isEqualTo(20);
        }
    }

    @Test
    public void getWithInvalidSignatures() throws RemoteException, BillingException {
        ArrayList<String> purchaseArray = mDataConverter.convertToPurchaseJsonArrayList(0, 5);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_PURCHASE_LIST, purchaseArray);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_SIGNATURE_LIST, mDataConverter.convertToInvalidSignatureRandomlyArrayList(purchaseArray));

        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                null
        )).thenReturn(bundle);

        Purchases purchases = null;
        try {
            purchases = mGetter.get(mService, Constants.TYPE_IN_APP);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_DATA);
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_GET_PURCHASE_VERIFICATION_FAILED);
        } finally {
            assertThat(purchases).isNull();
        }
    }

    @Test
    public void getWithJsonDataBroken() throws RemoteException {
        ArrayList<String> purchaseArray = mDataConverter.convertToPurchaseJsonBrokenArrayList();
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_PURCHASE_LIST, purchaseArray);
        bundle.putStringArrayList(Constants.RESPONSE_INAPP_SIGNATURE_LIST, mDataConverter.convertToSignatureArrayList(purchaseArray));

        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                null
        )).thenReturn(bundle);

        Purchases purchases = null;
        try {
            purchases = mGetter.get(mService, Constants.TYPE_IN_APP);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_BAD_RESPONSE);
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_BAD_RESPONSE);
        } finally {
            assertThat(purchases).isNull();
        }
    }

    @Test
    public void getWithLongResponseCode() throws RemoteException {
        Bundle bundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, 0, null);
        bundle.putLong(Constants.RESPONSE_CODE, 0L);

        getPurchases(bundle, -1, "");
    }

    @Test
    public void getWithDifferentResponseCode() throws RemoteException {
        Bundle bundle = mDataConverter.convertToPurchaseResponseBundle(3, 0, 0, null);

        getPurchases(bundle, 3, Constants.ERROR_MSG_GET_PURCHASES);
    }

    @Test
    public void getWithIntegerResponseCode() throws RemoteException {
        Bundle bundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, 0, null);

        getPurchases(bundle, -1, "");
    }

    @Test
    public void getWithNoResponseCode() throws RemoteException {
        Bundle bundle = mDataConverter.convertToPurchaseResponseBundle(0, 0, 0, null);

        getPurchases(bundle, -1, "");
    }

    @Test
    public void stringResponseCode() throws InterruptedException, RemoteException {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESPONSE_CODE, "0");

        getPurchases(bundle, Constants.ERROR_UNEXPECTED_TYPE, Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE);
    }

    private void getPurchases(Bundle bundle, int errorCode, String errorMessage) throws RemoteException {
        Mockito.when(mService.getPurchases(
                mBillingContext.getApiVersion(),
                mBillingContext.getContext().getPackageName(),
                Constants.TYPE_IN_APP,
                null
        )).thenReturn(bundle);

        Purchases purchases = null;
        try {
            purchases = mGetter.get(mService, Constants.TYPE_IN_APP);
        } catch (BillingException e) {
            assertThat(e.getErrorCode()).isEqualTo(errorCode);
            assertThat(e.getMessage()).isEqualTo(errorMessage);
        } finally {
            if (errorCode == -1) {
                assertThat(purchases).isNotNull();
            } else {
                assertThat(purchases).isNull();
            }
            verify(mService).getPurchases(
                    mBillingContext.getApiVersion(),
                    mBillingContext.getContext().getPackageName(),
                    Constants.TYPE_IN_APP,
                    null
            );
            verifyNoMoreInteractions(mService);
        }
    }
}