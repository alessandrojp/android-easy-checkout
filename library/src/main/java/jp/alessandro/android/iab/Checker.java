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
import android.os.Looper;
import android.text.TextUtils;

import java.util.ArrayList;

import jp.alessandro.android.iab.handler.ConsumeItemHandler;
import jp.alessandro.android.iab.handler.InventoryHandler;
import jp.alessandro.android.iab.handler.ItemDetailsHandler;
import jp.alessandro.android.iab.handler.PurchasesHandler;
import jp.alessandro.android.iab.handler.StartActivityHandler;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/26.
 */

class Checker {

    private Checker() {
    }

    public static void startActivityArguments(Activity activity,
                                              String itemId,
                                              PurchaseType purchaseType,
                                              StartActivityHandler handler) {

        if (activity == null || TextUtils.isEmpty(itemId) || purchaseType == null || handler == null) {
            throw new IllegalArgumentException(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    public static void consumePurchasesArguments(String itemId, ConsumeItemHandler handler) {
        if (TextUtils.isEmpty(itemId) || handler == null) {
            throw new IllegalArgumentException(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    public static void getItemDetailsArguments(PurchaseType purchaseType,
                                               ArrayList<String> itemIds,
                                               ItemDetailsHandler handler) {

        if (purchaseType == null || itemIds == null || itemIds.isEmpty() || handler == null) {
            throw new IllegalArgumentException(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    public static void getPurchasesArguments(PurchaseType purchaseType, PurchasesHandler handler) {

        if (purchaseType == null || handler == null) {
            throw new IllegalArgumentException(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    public static void getInventoryArguments(PurchaseType purchaseType, InventoryHandler handler) {

        if (purchaseType == null || handler == null) {
            throw new IllegalArgumentException(Constants.ERROR_MSG_ARGUMENT_MISSING);
        }
    }

    public static void isMainThread() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            return;
        }
        throw new IllegalStateException(Constants.ERROR_MSG_METHOD_MUST_BE_CALLED_ON_UI_THREAD);
    }
}