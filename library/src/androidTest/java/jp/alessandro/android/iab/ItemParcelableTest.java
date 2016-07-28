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
public class ItemParcelableTest {

    private final String mSkuDetailsJson = "{" +
            "\"productId\": \"premium\"," +
            "\"type\": \"subs\"," +
            "\"price\": \"¥960\"," +
            "\"price_amount_micros\": \"9600000\"," +
            "\"price_currency_code\": \"JPY\"," +
            "\"title\": \"Test Product\"," +
            "\"description\": \"Fast and easy use Android In-App Billing\"}";

    @Test
    public void testParcelable() throws Exception {
        Item item = Item.parseJson(mSkuDetailsJson);

        assertNotNull(item);

        Parcel parcel = Parcel.obtain();
        item.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Item fromParcel = Item.CREATOR.createFromParcel(parcel);

        assertEquals(item.getOriginalJson(), fromParcel.getOriginalJson());
        assertEquals(item.getSku(), fromParcel.getSku());
        assertEquals(item.getType(), fromParcel.getType());
        assertEquals(item.getTitle(), fromParcel.getTitle());
        assertEquals(item.getDescription(), fromParcel.getDescription());
        assertEquals(item.getCurrency(), fromParcel.getCurrency());
        assertEquals(item.getPrice(), fromParcel.getPrice());
        assertEquals(item.getPriceMicros(), fromParcel.getPriceMicros());
    }
}