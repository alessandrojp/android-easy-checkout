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
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an in-app product's listing details.
 */
public class Item implements Parcelable {

    private final String mOriginalJson;
    private final String mSku;
    private final String mType;
    private final String mTitle;
    private final String mDescription;
    private final String mCurrency;
    private final String mPrice;
    private final long mPriceMicros;

    public Item(String originalJson, String sku, String type, String title,
                String description, String currency, String price, long priceMicros) {
        mOriginalJson = originalJson;
        mSku = sku;
        mType = type;
        mTitle = title;
        mDescription = description;
        mCurrency = currency;
        mPrice = price;
        mPriceMicros = priceMicros;
    }

    public static Item parseJson(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return new Item(json, obj.optString("productId"), obj.optString("type"), obj.optString("title"),
                obj.optString("description"), obj.optString("price_currency_code"), obj.optString("price"),
                obj.optLong("price_amount_micros"));
    }

    protected Item(Parcel in) {
        mOriginalJson = in.readString();
        mSku = in.readString();
        mType = in.readString();
        mTitle = in.readString();
        mDescription = in.readString();
        mCurrency = in.readString();
        mPrice = in.readString();
        mPriceMicros = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mOriginalJson);
        dest.writeString(mSku);
        dest.writeString(mType);
        dest.writeString(mTitle);
        dest.writeString(mDescription);
        dest.writeString(mCurrency);
        dest.writeString(mPrice);
        dest.writeLong(mPriceMicros);
    }

    public String getOriginalJson() {
        return mOriginalJson;
    }

    public String getSku() {
        return mSku;
    }

    public String getType() {
        return mType;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getCurrency() {
        return mCurrency;
    }

    public String getPrice() {
        return mPrice;
    }

    public long getPriceMicros() {
        return mPriceMicros;
    }

    public static final Creator<Item> CREATOR = new Creator<Item>() {
        public Item createFromParcel(Parcel source) {
            return new Item(source);
        }

        public Item[] newArray(int size) {
            return new Item[size];
        }
    };
}