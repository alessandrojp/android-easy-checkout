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

import jp.alessandro.android.iab.handler.PurchaseHandler;

class PurchaseLaunchState {

    private boolean mIsLocked = false;
    private PurchaseHandler mHandler;

    public PurchaseHandler getHandler() {
        synchronized (this) {
            return mHandler;
        }
    }

    public void setHandler(PurchaseHandler handler) {
        synchronized (this) {
            mHandler = handler;
        }
    }

    public boolean tryLock() {
        synchronized (this) {
            if (mIsLocked) {
                return false;
            }
            mIsLocked = true;
            mHandler = null;
            return true;
        }
    }

    public PurchaseLaunchState tryUnlock() {
        synchronized (this) {
            if (!mIsLocked) {
                return null;
            }
            mIsLocked = false;
            PurchaseLaunchState state = new PurchaseLaunchState();
            state.setHandler(mHandler);
            mHandler = null;
            return state;
        }
    }
}