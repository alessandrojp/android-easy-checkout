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

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Locale;

import jp.alessandro.android.iab.util.DataConverter;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Created by Alessandro Yuichi Okimoto on 2016/07/23.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class ItemParcelableTest {

    @Test
    public void writeToParcel() throws JSONException {
        Item item = Item.parseJson(String.format(Locale.ENGLISH, DataConverter.SKU_DETAIL_JSON, 0));

        // Obtain a Parcel object and write the parcelable object to it
        Parcel parcel = Parcel.obtain();
        item.writeToParcel(parcel, item.describeContents());

        // After you're done with writing, you need to reset the parcel for reading
        parcel.setDataPosition(0);

        Item fromParcel = Item.CREATOR.createFromParcel(parcel);

        assertThat(item.getOriginalJson()).isEqualTo(fromParcel.getOriginalJson());
        assertThat(item.getSku()).isEqualTo(fromParcel.getSku());
        assertThat(item.getType()).isEqualTo(fromParcel.getType());
        assertThat(item.getTitle()).isEqualTo(fromParcel.getTitle());
        assertThat(item.getDescription()).isEqualTo(fromParcel.getDescription());
        assertThat(item.getCurrency()).isEqualTo(fromParcel.getCurrency());
        assertThat(item.getPrice()).isEqualTo(fromParcel.getPrice());
        assertThat(item.getPriceMicros()).isEqualTo(fromParcel.getPriceMicros());
    }

    @Test
    public void newArray() {
        Item[] items = Item.CREATOR.newArray(10);
        assertThat(items.length).isEqualTo(10);
    }
}