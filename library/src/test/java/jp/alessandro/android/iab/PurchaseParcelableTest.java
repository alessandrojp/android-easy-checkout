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

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import jp.alessandro.android.iab.util.DataConverter;
import jp.alessandro.android.iab.util.DataSigner;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/19.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class PurchaseParcelableTest {

    @Test
    public void writeToParcelAutoRenewingTrue() throws JSONException {
        Purchase purchase = Purchase.parseJson(
                DataConverter.TEST_JSON_RECEIPT,
                new DataSigner().sign(DataConverter.TEST_JSON_RECEIPT, Security.KEY_FACTORY_ALGORITHM, Security.SIGNATURE_ALGORITHM));

        writeToParcel(purchase);
    }

    @Test
    public void writeToParcelAutoRenewingFalse() throws JSONException {
        String signature = new DataSigner().sign(
                DataConverter.TEST_JSON_RECEIPT_AUTO_RENEWING_FALSE,
                Security.KEY_FACTORY_ALGORITHM,
                Security.SIGNATURE_ALGORITHM);

        Purchase purchase = Purchase.parseJson(
                DataConverter.TEST_JSON_RECEIPT_AUTO_RENEWING_FALSE,
                signature);

        writeToParcel(purchase);
    }

    @Test
    public void newArray() {
        Purchase[] items = Purchase.CREATOR.newArray(10);
        assertThat(items.length).isEqualTo(10);
    }

    private void writeToParcel(Purchase purchase) {
        // Obtain a Parcel object and write the parcelable object to it
        Parcel parcel = Parcel.obtain();
        purchase.writeToParcel(parcel, purchase.describeContents());

        // After you're done with writing, you need to reset the parcel for reading
        parcel.setDataPosition(0);

        Purchase fromParcel = Purchase.CREATOR.createFromParcel(parcel);

        assertThat(purchase.getOriginalJson()).isEqualTo(fromParcel.getOriginalJson());
        assertThat(purchase.getOrderId()).isEqualTo(fromParcel.getOrderId());
        assertThat(purchase.getPackageName()).isEqualTo(fromParcel.getPackageName());
        assertThat(purchase.getSku()).isEqualTo(fromParcel.getSku());
        assertThat(purchase.getPurchaseTime()).isEqualTo(fromParcel.getPurchaseTime());
        assertThat(purchase.getPurchaseState()).isEqualTo(fromParcel.getPurchaseState());
        assertThat(purchase.getDeveloperPayload()).isEqualTo(fromParcel.getDeveloperPayload());
        assertThat(purchase.getToken()).isEqualTo(fromParcel.getToken());
        assertThat(purchase.isAutoRenewing()).isEqualTo(fromParcel.isAutoRenewing());
        assertThat(purchase.getSignature()).isEqualTo(fromParcel.getSignature());
    }
}