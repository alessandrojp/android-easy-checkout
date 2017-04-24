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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import jp.alessandro.android.iab.logger.CustomLogger;
import jp.alessandro.android.iab.logger.DiscardLogger;
import jp.alessandro.android.iab.logger.Logger;
import jp.alessandro.android.iab.logger.SystemLogger;

import static org.mockito.Mockito.verify;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/26.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class LoggerTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Spy
    CustomLogger mCustomLogger;
    @Spy
    DiscardLogger mDiscardLogger;
    @Spy
    SystemLogger mSystemLogger;

    @Test
    public void debug() {
        String message = "test";

        mCustomLogger.d(Logger.TAG, message);
        mDiscardLogger.d(Logger.TAG, message);
        mSystemLogger.d(Logger.TAG, message);

        verify(mCustomLogger).d(Logger.TAG, message);
        verify(mDiscardLogger).d(Logger.TAG, message);
        verify(mSystemLogger).d(Logger.TAG, message);
    }

    @Test
    public void error() {
        String message = "test";

        mCustomLogger.e(Logger.TAG, message);
        mDiscardLogger.e(Logger.TAG, message);
        mSystemLogger.e(Logger.TAG, message);

        verify(mCustomLogger).e(Logger.TAG, message);
        verify(mDiscardLogger).e(Logger.TAG, message);
        verify(mSystemLogger).e(Logger.TAG, message);
    }

    @Test
    public void errorWithException() {
        String message = "test";
        Exception e = new Exception();

        mCustomLogger.e(Logger.TAG, message, e);
        mDiscardLogger.e(Logger.TAG, message, e);
        mSystemLogger.e(Logger.TAG, message, e);

        verify(mCustomLogger).e(Logger.TAG, message, e);
        verify(mDiscardLogger).e(Logger.TAG, message, e);
        verify(mSystemLogger).e(Logger.TAG, message, e);
    }

    @Test
    public void info() {
        String message = "test";

        mCustomLogger.i(Logger.TAG, message);
        mDiscardLogger.i(Logger.TAG, message);
        mSystemLogger.i(Logger.TAG, message);

        verify(mCustomLogger).i(Logger.TAG, message);
        verify(mDiscardLogger).i(Logger.TAG, message);
        verify(mSystemLogger).i(Logger.TAG, message);
    }

    @Test
    public void verbose() {
        String message = "test";

        mCustomLogger.v(Logger.TAG, message);
        mDiscardLogger.v(Logger.TAG, message);
        mSystemLogger.v(Logger.TAG, message);

        verify(mCustomLogger).v(Logger.TAG, message);
        verify(mDiscardLogger).v(Logger.TAG, message);
        verify(mSystemLogger).v(Logger.TAG, message);
    }

    @Test
    public void warning() {
        String message = "test";

        mCustomLogger.w(Logger.TAG, message);
        mDiscardLogger.w(Logger.TAG, message);
        mSystemLogger.w(Logger.TAG, message);

        verify(mCustomLogger).w(Logger.TAG, message);
        verify(mDiscardLogger).w(Logger.TAG, message);
        verify(mSystemLogger).w(Logger.TAG, message);
    }
}