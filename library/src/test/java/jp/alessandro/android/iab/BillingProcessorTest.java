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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.alessandro.android.iab.handler.ConsumeItemHandler;
import jp.alessandro.android.iab.handler.InventoryHandler;
import jp.alessandro.android.iab.handler.ItemDetailsHandler;
import jp.alessandro.android.iab.handler.PurchasesHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class BillingProcessorTest {

    private Handler mWorkHandler;
    private BillingProcessor mProcessor;

    private final BillingContext mContext = Util.newBillingContext(RuntimeEnvironment.application);

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    BillingService mService;
    @Mock
    ServiceBinder mServiceBinder;
    @Mock
    Activity mActivity;

    @Before
    public void setUp() {
        HandlerThread thread = new HandlerThread(BillingProcessor.WORK_THREAD_NAME);
        thread.start();
        // Handler to post all actions in the library
        mWorkHandler = new Handler(thread.getLooper());

        mProcessor = spy(new BillingProcessor(mContext, null));
    }

    @Test
    public void isServiceAvailable() {
        assertThat(mProcessor.isServiceAvailable(mContext.getContext())).isFalse();
    }

    @Test
    public void onActivityResult() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int requestCode = 1001;
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);
        PurchaseType type = PurchaseType.IN_APP;

        setUpStartPurchase(bundle, type);
        mProcessor.startPurchase(
                mActivity,
                requestCode,
                Constants.TEST_PRODUCT_ID,
                type,
                Constants.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        assertThat(mProcessor.onActivityResult(requestCode, -1, Util.newOkIntent())).isTrue();
                        latch.countDown();
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException();
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void onActivityResultError() {
        assertThat(mProcessor.onActivityResult(0, 0, null)).isFalse();
    }

    @Test
    public void startPurchaseInApp() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);
        PurchaseType type = PurchaseType.IN_APP;

        startActivity(latch, bundle, requestCode, type);

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void startPurchaseInAppError() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        PurchaseType type = PurchaseType.IN_APP;

        startActivityError(latch, bundle, requestCode, type);

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void updateSubscription() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        List<String> oldItemIds = new ArrayList<>();
        oldItemIds.add(Constants.TEST_PRODUCT_ID);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);

        setUpUpdateSubscriptionPurchase(bundle, oldItemIds);
        mProcessor.updateSubscription(
                mActivity,
                requestCode,
                oldItemIds,
                Constants.TEST_PRODUCT_ID,
                Constants.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException();
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void updateSubscriptionError() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;
        List<String> oldItemIds = new ArrayList<>();
        oldItemIds.add(Constants.TEST_PRODUCT_ID);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);

        setUpUpdateSubscriptionPurchase(bundle, oldItemIds);
        mProcessor.updateSubscription(
                mActivity,
                requestCode,
                oldItemIds,
                Constants.TEST_PRODUCT_ID,
                Constants.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        throw new IllegalStateException();
                    }

                    @Override
                    public void onError(BillingException e) {
                        assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PENDING_INTENT);
                        assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PENDING_INTENT);
                        latch.countDown();
                    }
                });
        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void updateSubscriptionListEmpty() {
        List<String> oldItemIds = new ArrayList<>();
        try {
            mProcessor.updateSubscription(null, 0, oldItemIds, null, null, null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_UPDATE_ARGUMENT_MISSING);
        }
    }

    @Test
    public void updateSubscriptionListNull() {
        try {
            mProcessor.updateSubscription(null, 0, null, null, null, null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_UPDATE_ARGUMENT_MISSING);
        }
    }

    @Test
    public void startPurchaseSubscription() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext.getContext(), 1, new Intent(), 0);
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        bundle.putParcelable(Constants.RESPONSE_BUY_INTENT, pendingIntent);
        PurchaseType type = PurchaseType.SUBSCRIPTION;

        startActivity(latch, bundle, requestCode, type);

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void startPurchaseSubscriptionError() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        int requestCode = 1001;
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.RESPONSE_CODE, 0L);
        PurchaseType type = PurchaseType.SUBSCRIPTION;

        startActivityError(latch, bundle, requestCode, type);

        latch.await(15, TimeUnit.SECONDS);
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
    public void getPurchasesAndRelease() throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int size = 10;
        Bundle responseBundle = Util.createPurchaseBundle(0, 0, size, null);

        doReturn(responseBundle).when(mService).getPurchases(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), Constants.ITEM_TYPE_SUBSCRIPTION, null);

        setUpProcessor(PurchaseType.SUBSCRIPTION);
        mProcessor.getPurchases(PurchaseType.SUBSCRIPTION, new PurchasesHandler() {
            @Override
            public void onSuccess(Purchases purchases) {
                mProcessor.release();
                try {
                    mProcessor.getPurchases(PurchaseType.SUBSCRIPTION, null);
                } catch (IllegalStateException e) {
                    assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_LIBRARY_ALREADY_RELEASED);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void getInAppPurchases() throws InterruptedException, RemoteException {
        getPurchases(PurchaseType.IN_APP);
    }

    @Test
    public void getSubscriptionPurchases() throws InterruptedException, RemoteException {
        getPurchases(PurchaseType.SUBSCRIPTION);
    }

    private void getPurchases(PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int size = 10;
        Bundle responseBundle = Util.createPurchaseBundle(0, 0, size, null);

        doReturn(responseBundle).when(mService).getPurchases(
                mContext.getApiVersion(),
                mContext.getContext().getPackageName(),
                type == PurchaseType.SUBSCRIPTION ? Constants.ITEM_TYPE_SUBSCRIPTION : Constants.TYPE_IN_APP,
                null);

        setUpProcessor(type);
        getPurchases(type, latch, size);

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void getInAppPurchasesError() throws InterruptedException, RemoteException {
        getPurchasesError(PurchaseType.IN_APP);
    }

    @Test
    public void getSubscriptionPurchasesError() throws InterruptedException, RemoteException {
        getPurchasesError(PurchaseType.SUBSCRIPTION);
    }

    private void getPurchasesError(PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);

        Bundle responseBundle = new Bundle();
        responseBundle.putInt(Constants.RESPONSE_CODE, 0);

        doReturn(responseBundle).when(mService).getPurchases(
                mContext.getApiVersion(),
                mContext.getContext().getPackageName(),
                type == PurchaseType.SUBSCRIPTION ? Constants.ITEM_TYPE_SUBSCRIPTION : Constants.TYPE_IN_APP,
                null);

        setUpProcessor(type);
        getPurchasesError(type, latch);

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    @Deprecated
    public void getInventorySubscription() throws InterruptedException, RemoteException {
        getInventory(PurchaseType.SUBSCRIPTION);
    }

    @Test
    @Deprecated
    public void getInventoryInApp() throws InterruptedException, RemoteException {
        getInventory(PurchaseType.IN_APP);
    }

    @Test
    public void getInAppItemDetails() throws InterruptedException, RemoteException {
        getItemDetails(PurchaseType.IN_APP);
    }

    @Test
    public void getSubscriptionItemDetails() throws InterruptedException, RemoteException {
        getItemDetails(PurchaseType.SUBSCRIPTION);
    }

    private void getItemDetails(PurchaseType purchaseType) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);

        ArrayList<String> itemIds = new ArrayList<>();
        Bundle requestBundle = new Bundle();
        requestBundle.putStringArrayList(Constants.RESPONSE_ITEM_ID_LIST, itemIds);

        final int size = 10;
        ArrayList<String> items = Util.createSkuItemDetailsJsonArray(size);
        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);
        responseBundle.putStringArrayList(Constants.RESPONSE_DETAILS_LIST, items);

        doReturn(requestBundle).when(mProcessor).createBundleItemListFromArray(itemIds);
        doReturn(responseBundle).when(mService).getSkuDetails(
                mContext.getApiVersion(),
                mContext.getContext().getPackageName(),
                purchaseType == PurchaseType.IN_APP ? Constants.ITEM_TYPE_INAPP : Constants.ITEM_TYPE_SUBSCRIPTION,
                requestBundle
        );
        setUpProcessor(purchaseType);
        getItemDetails(purchaseType, latch, itemIds, size);

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void getInAppItemDetailsError() throws InterruptedException, RemoteException {
        getItemDetailsError(PurchaseType.IN_APP);
    }

    @Test
    public void getSubscriptionItemDetailsError() throws InterruptedException, RemoteException {
        getItemDetailsError(PurchaseType.IN_APP);
    }

    private void getItemDetailsError(PurchaseType purchaseType) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);

        ArrayList<String> itemIds = new ArrayList<>();
        Bundle requestBundle = new Bundle();
        requestBundle.putStringArrayList(Constants.RESPONSE_ITEM_ID_LIST, itemIds);

        Bundle responseBundle = new Bundle();
        responseBundle.putLong(Constants.RESPONSE_CODE, 0L);

        doReturn(requestBundle).when(mProcessor).createBundleItemListFromArray(itemIds);
        doReturn(responseBundle).when(mService).getSkuDetails(
                mContext.getApiVersion(),
                mContext.getContext().getPackageName(),
                purchaseType == PurchaseType.IN_APP ? Constants.ITEM_TYPE_INAPP : Constants.ITEM_TYPE_SUBSCRIPTION,
                requestBundle
        );
        setUpProcessor(PurchaseType.IN_APP);
        getItemDetailsError(PurchaseType.IN_APP, latch, itemIds);

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consume() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String purchaseToken = "purchase_token";

        Bundle responseBundle = new Bundle();
        responseBundle.putInt(Constants.RESPONSE_CODE, 0);

        doReturn(0).when(mService).consumePurchase(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), purchaseToken);

        doReturn(purchaseToken).when(mProcessor).getToken(mService, Constants.TEST_PRODUCT_ID);

        setUpProcessor(PurchaseType.IN_APP);
        mProcessor.consume(Constants.TEST_PRODUCT_ID, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                try {
                    verify(mService).consumePurchase(
                            mContext.getApiVersion(), mContext.getContext().getPackageName(), purchaseToken);
                } catch (RemoteException e2) {
                } finally {
                    verifyNoMoreInteractions(mService);
                    latch.countDown();
                }
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void consumePurchase() throws InterruptedException, RemoteException, BillingException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String purchaseToken = "purchase_token";

        Bundle responseBundle = new Bundle();
        responseBundle.putInt(Constants.RESPONSE_CODE, 0);

        doReturn(0).when(mService).consumePurchase(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), purchaseToken);

        doReturn(purchaseToken).when(mProcessor).getToken(mService, Constants.TEST_PRODUCT_ID);

        setUpProcessor(PurchaseType.IN_APP);
        mProcessor.consumePurchase(Constants.TEST_PRODUCT_ID, new ConsumeItemHandler() {
            @Override
            public void onSuccess() {
                try {
                    verify(mService).consumePurchase(
                            mContext.getApiVersion(), mContext.getContext().getPackageName(), purchaseToken);
                } catch (RemoteException e2) {
                } finally {
                    verifyNoMoreInteractions(mService);
                    latch.countDown();
                }
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();

        latch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void getWorkHandler() {
        Handler handler = mProcessor.getWorkHandler();
        assertThat(handler.getLooper().getThread().getName()).isEqualTo(BillingProcessor.WORK_THREAD_NAME);
    }

    @Test
    public void createServiceBinder() {
        ServiceBinder binder = mProcessor.createServiceBinder();
        assertThat(binder).isNotNull();
    }

    private void startActivity(final CountDownLatch latch,
                               Bundle bundle,
                               int requestCode,
                               PurchaseType type) throws RemoteException {
        setUpStartPurchase(bundle, type);
        mProcessor.startPurchase(
                mActivity,
                requestCode,
                Constants.TEST_PRODUCT_ID,
                type,
                Constants.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(BillingException e) {
                        throw new IllegalStateException();
                    }
                });
    }

    private void startActivityError(final CountDownLatch latch,
                                    Bundle bundle,
                                    int requestCode,
                                    PurchaseType type) throws RemoteException {
        setUpStartPurchase(bundle, type);
        mProcessor.startPurchase(
                mActivity,
                requestCode,
                Constants.TEST_PRODUCT_ID,
                type,
                Constants.TEST_DEVELOPER_PAYLOAD,
                new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                        throw new IllegalStateException();
                    }

                    @Override
                    public void onError(BillingException e) {
                        assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PENDING_INTENT);
                        assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_PENDING_INTENT);
                        latch.countDown();
                    }
                });
    }

    private void setUpStartPurchase(Bundle bundle, PurchaseType type) throws RemoteException {
        setUpProcessor(type);
        Mockito.when(mService.getBuyIntent(
                mContext.getApiVersion(),
                mContext.getContext().getPackageName(),
                Constants.TEST_PRODUCT_ID,
                type == PurchaseType.SUBSCRIPTION ? Constants.TYPE_SUBSCRIPTION : Constants.TYPE_IN_APP,
                Constants.TEST_DEVELOPER_PAYLOAD
        )).thenReturn(bundle);
    }

    private void setUpUpdateSubscriptionPurchase(Bundle bundle, List<String> oldItems) throws RemoteException {
        setUpProcessor(PurchaseType.SUBSCRIPTION);
        Mockito.when(mService.getBuyIntentToReplaceSkus(
                BillingApi.VERSION_5.getValue(),
                mContext.getContext().getPackageName(),
                oldItems,
                Constants.TEST_PRODUCT_ID,
                Constants.TYPE_SUBSCRIPTION,
                Constants.TEST_DEVELOPER_PAYLOAD
        )).thenReturn(bundle);
    }

    private void getPurchases(final PurchaseType type, final CountDownLatch latch, final int size) {
        mProcessor.getPurchases(type, new PurchasesHandler() {
            @Override
            public void onSuccess(Purchases purchases) {
                assertThat(purchases).isNotNull();
                assertThat(purchases.getSize()).isEqualTo(size);
                assertThat(purchases.getAll()).isNotNull();

                List<Purchase> purchaseList = purchases.getAll();
                for (Purchase p : purchaseList) {
                    assertThat(purchases.hasItemId(p.getSku())).isTrue();
                    assertThat(purchases.getByPurchaseId(p.getSku())).isNotNull();
                }
                mProcessor.release();
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();
    }

    private void getPurchasesError(PurchaseType type, final CountDownLatch latch) {
        mProcessor.getPurchases(type, new PurchasesHandler() {
            @Override
            public void onSuccess(Purchases purchases) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e).isNotNull();
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_PURCHASE_DATA);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_GET_PURCHASES_DATA_LIST);
                mProcessor.release();
                latch.countDown();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();
    }

    private void getItemDetails(PurchaseType type, final CountDownLatch latch, final ArrayList<String> itemIds, final int size) {
        mProcessor.getItemDetails(type, itemIds, new ItemDetailsHandler() {
            @Override
            public void onSuccess(ItemDetails itemDetails) {
                assertThat(itemDetails.getSize()).isEqualTo(size);
                assertThat(itemDetails.getAll()).isNotNull();

                List<Item> purchaseList = itemDetails.getAll();
                for (Item item : purchaseList) {
                    assertThat(itemDetails.hasItemId(item.getSku())).isTrue();
                    assertThat(itemDetails.getByItemId(item.getSku())).isNotNull();
                }
                mProcessor.release();
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();
    }

    private void getItemDetailsError(PurchaseType type, final CountDownLatch latch, final ArrayList<String> itemIds) {
        mProcessor.getItemDetails(type, itemIds, new ItemDetailsHandler() {
            @Override
            public void onSuccess(ItemDetails itemDetails) {
                throw new IllegalStateException();
            }

            @Override
            public void onError(BillingException e) {
                assertThat(e).isNotNull();
                assertThat(e.getErrorCode()).isEqualTo(Constants.ERROR_UNEXPECTED_TYPE);
                assertThat(e.getMessage()).isEqualTo(Constants.ERROR_MSG_GET_SKU_DETAILS_RESPONSE_LIST_NULL);
                mProcessor.release();
                latch.countDown();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();
    }

    private void getInventory(final PurchaseType type) throws InterruptedException, RemoteException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int size = 10;
        Bundle responseBundle = Util.createPurchaseBundle(0, 0, size, null);
        String purchaseType = type == PurchaseType.SUBSCRIPTION ? Constants.ITEM_TYPE_SUBSCRIPTION : Constants.ITEM_TYPE_INAPP;
        doReturn(responseBundle).when(mService).getPurchases(
                mContext.getApiVersion(), mContext.getContext().getPackageName(), purchaseType, null);

        setUpProcessor(type);
        mProcessor.getInventory(type, new InventoryHandler() {
            @Override
            public void onSuccess(Purchases purchases) {
                assertThat(purchases).isNotNull();
                assertThat(purchases.getSize()).isEqualTo(size);
                assertThat(purchases.getAll()).isNotNull();

                List<Purchase> purchaseList = purchases.getAll();
                for (Purchase p : purchaseList) {
                    assertThat(purchases.hasItemId(p.getSku())).isTrue();
                    assertThat(purchases.getByPurchaseId(p.getSku())).isNotNull();
                }
                latch.countDown();
            }

            @Override
            public void onError(BillingException e) {
                throw new IllegalStateException();
            }
        });
        shadowOf(mWorkHandler.getLooper()).getScheduler().advanceToNextPostedRunnable();
    }

    private void setUpProcessor(PurchaseType purchaseType) {
        doReturn(mWorkHandler).when(mProcessor).getWorkHandler();
        doReturn(true).when(mProcessor).isSupported(purchaseType, mService);
        doReturn(mServiceBinder).when(mProcessor).createServiceBinder();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ServiceBinder.Handler handler = invocation.getArgument(0);
                handler.onBind(mService);
                return null;
            }
        }).when(mServiceBinder).getServiceAsync(any(ServiceBinder.Handler.class));
    }
}