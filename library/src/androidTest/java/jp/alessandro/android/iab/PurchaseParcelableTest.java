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

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Alessandro Yuichi Okimoto on 2016/07/23.
 */
@RunWith(AndroidJUnit4.class)
public class PurchaseParcelableTest {

    private final String mPurchaseDataJson = "{" +
            "\"orderId\": \"GPA.1111-2222-3333-44444\"," +
            "\"packageName\": \"jp.alessandro.android.iab\"," +
            "\"productId\": \"premium\"," +
            "\"purchaseTime\": 1469232000," +
            "\"purchaseState\": 0," +
            "\"developerPayload\": \"developer_payload\"," +
            "\"purchaseToken\": \"token\"," +
            "\"autoRenewing\": true}";

    @Test
    public void testParcelable() throws Exception {
        Purchase purchase = Purchase.parseJson(mPurchaseDataJson, "signature");

        assertNotNull(purchase);

        Parcel parcel = Parcel.obtain();
        purchase.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Purchase fromParcel = Purchase.CREATOR.createFromParcel(parcel);

        assertEquals(purchase.getOriginalJson(), fromParcel.getOriginalJson());
        assertEquals(purchase.getOrderId(), fromParcel.getOrderId());
        assertEquals(purchase.getPackageName(), fromParcel.getPackageName());
        assertEquals(purchase.getSku(), fromParcel.getSku());
        assertEquals(purchase.getPurchaseTime(), fromParcel.getPurchaseTime());
        assertEquals(purchase.getPurchaseState(), fromParcel.getPurchaseState());
        assertEquals(purchase.getDeveloperPayload(), fromParcel.getDeveloperPayload());
        assertEquals(purchase.getToken(), fromParcel.getToken());
        assertEquals(purchase.isAutoRenewing(), fromParcel.isAutoRenewing());
        assertEquals(purchase.getSignature(), fromParcel.getSignature());
    }
}