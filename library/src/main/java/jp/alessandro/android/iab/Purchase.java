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
 * Represents an in-app billing purchase.
 */
public class Purchase implements Parcelable {

    private final String mOriginalJson;
    private final String mOrderId;
    private final String mPackageName;
    private final String mSku;
    private final long mPurchaseTime;
    private final int mPurchaseState;
    private final String mDeveloperPayload;
    private final String mToken;
    private final boolean mAutoRenewing;
    private final String mSignature;

    public Purchase(String originalJson, String orderId, String packageName, String sku,
                    long purchaseTime, int purchaseState, String developerPayload, String token,
                    boolean autoRenewing, String signature) {
        mOriginalJson = originalJson;
        mOrderId = orderId;
        mPackageName = packageName;
        mSku = sku;
        mPurchaseTime = purchaseTime;
        mPurchaseState = purchaseState;
        mDeveloperPayload = developerPayload;
        mToken = token;
        mAutoRenewing = autoRenewing;
        mSignature = signature;
    }

    public static Purchase parseJson(String json, String signature) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return new Purchase(json, obj.optString("orderId"), obj.optString("packageName"),
                obj.optString("productId"), obj.optLong("purchaseTime"), obj.optInt("purchaseState"),
                obj.optString("developerPayload"), obj.optString("token", obj.optString("purchaseToken")),
                obj.optBoolean("autoRenewing"), signature);
    }

    protected Purchase(Parcel in) {
        mOriginalJson = in.readString();
        mOrderId = in.readString();
        mPackageName = in.readString();
        mSku = in.readString();
        mPurchaseTime = in.readLong();
        mPurchaseState = in.readInt();
        mDeveloperPayload = in.readString();
        mToken = in.readString();
        mAutoRenewing = in.readByte() != 0;
        mSignature = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mOriginalJson);
        dest.writeString(mOrderId);
        dest.writeString(mPackageName);
        dest.writeString(mSku);
        dest.writeLong(mPurchaseTime);
        dest.writeInt(mPurchaseState);
        dest.writeString(mDeveloperPayload);
        dest.writeString(mToken);
        dest.writeByte(mAutoRenewing ? (byte) 1 : (byte) 0);
        dest.writeString(mSignature);
    }

    public String getOriginalJson() {
        return mOriginalJson;
    }

    public String getOrderId() {
        return mOrderId;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getSku() {
        return mSku;
    }

    public long getPurchaseTime() {
        return mPurchaseTime;
    }

    public int getPurchaseState() {
        return mPurchaseState;
    }

    public String getDeveloperPayload() {
        return mDeveloperPayload;
    }

    public String getToken() {
        return mToken;
    }

    public boolean isAutoRenewing() {
        return mAutoRenewing;
    }

    public String getSignature() {
        return mSignature;
    }

    public static final Creator<Purchase> CREATOR = new Creator<Purchase>() {
        public Purchase createFromParcel(Parcel source) {
            return new Purchase(source);
        }

        public Purchase[] newArray(int size) {
            return new Purchase[size];
        }
    };
}