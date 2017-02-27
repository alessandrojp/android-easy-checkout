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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import jp.alessandro.android.iab.logger.DiscardLogger;
import jp.alessandro.android.iab.logger.Logger;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/26.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class SecurityTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private Security mSecurity;

    @Before
    public void setUp() {
        mSecurity = spy(new Security());
    }

    @Test
    public void verifyPurchaseSuccess() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = Constants.TEST_PUBLIC_KEY_BASE_64;
        String signedData = Constants.TEST_JSON_RECEIPT;
        String signature = DataSigner.sign(signedData);

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isTrue();
    }

    @Test
    public void verifyPurchaseFailed() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = Constants.TEST_PUBLIC_KEY_BASE_64;
        String signedData = Constants.TEST_JSON_RECEIPT;
        String signedDifferentData = Constants.TEST_JSON_RECEIPT_AUTO_RENEWING_FALSE;
        String signature = DataSigner.sign(signedDifferentData);

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
    }

    @Test
    public void verifyPurchaseBase64PublicKeyEmpty() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "";
        String signedData = "";
        String signature = "";

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
    }

    @Test
    public void verifyPurchaseSignedDataEmpty() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "";
        String signature = "";

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
    }

    @Test
    public void verifyPurchaseSignatureEmpty() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "signedData";
        String signature = "";

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
    }

    @Test
    public void verifyPurchaseStaticResponsePurchased() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "{\"productId\": \"android.test.purchased\"}";
        String signature = "";

        if (BuildConfig.DEBUG) {
            assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isTrue();
        } else {
            assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        }
    }

    @Test
    public void verifyPurchaseStaticResponseCanceled() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "{\"productId\": \"android.test.canceled\"}";
        String signature = "";

        if (BuildConfig.DEBUG) {
            assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isTrue();
        } else {
            assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        }
    }

    @Test
    public void verifyPurchaseStaticResponseRefunded() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "{\"productId\": \"android.test.refunded\"}";
        String signature = "";

        if (BuildConfig.DEBUG) {
            assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isTrue();
        } else {
            assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        }
    }

    @Test
    public void verifyPurchaseStaticResponseItemUnavailable() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "{\"productId\": \"android.test.item_unavailable\"}";
        String signature = "";

        if (BuildConfig.DEBUG) {
            assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isTrue();
        } else {
            assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        }
    }

    @Test
    public void generatePublicKeyNoSuchAlgorithmException()
            throws NoSuchAlgorithmException, InvalidKeySpecException, IllegalArgumentException {

        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "signedData";
        String signature = "signature";

        doThrow(new NoSuchAlgorithmException()).when(mSecurity).generatePublicKey(base64PublicKey);

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        verify(mSecurity).generatePublicKey(base64PublicKey);
    }

    @Test
    public void generatePublicKeyInvalidKeySpecException()
            throws NoSuchAlgorithmException, InvalidKeySpecException, IllegalArgumentException {

        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "signedData";
        String signature = "signature";

        doThrow(new InvalidKeySpecException()).when(mSecurity).generatePublicKey(base64PublicKey);

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        verify(mSecurity).generatePublicKey(base64PublicKey);
    }

    @Test
    public void generatePublicKeyIllegalArgumentException()
            throws NoSuchAlgorithmException, InvalidKeySpecException, IllegalArgumentException {

        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "signedData";
        String signature = "signature";

        doThrow(new IllegalArgumentException()).when(mSecurity).generatePublicKey(base64PublicKey);

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        verify(mSecurity).generatePublicKey(base64PublicKey);
    }

    @Test
    public void verifyUnsupportedEncodingException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        doReturn(publicKey).when(mSecurity).generatePublicKey(anyString());
        doThrow(new UnsupportedEncodingException()).when(mSecurity).verify(logger, publicKey, signedData, signature);

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        verify(mSecurity).verify(logger, publicKey, signedData, signature);
    }

    @Test
    public void verifyNoSuchAlgorithmException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        doReturn(publicKey).when(mSecurity).generatePublicKey(anyString());
        doThrow(new NoSuchAlgorithmException()).when(mSecurity).verify(logger, publicKey, signedData, signature);

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        verify(mSecurity).verify(logger, publicKey, signedData, signature);
    }

    @Test
    public void verifyInvalidKeyException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        doReturn(publicKey).when(mSecurity).generatePublicKey(anyString());
        doThrow(new InvalidKeyException()).when(mSecurity).verify(logger, publicKey, signedData, signature);

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        verify(mSecurity).verify(logger, publicKey, signedData, signature);
    }

    @Test
    public void verifyInvalidKeySpecException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        doReturn(publicKey).when(mSecurity).generatePublicKey(anyString());
        doThrow(new InvalidKeySpecException()).when(mSecurity).verify(logger, publicKey, signedData, signature);

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        verify(mSecurity).verify(logger, publicKey, signedData, signature);
    }

    @Test
    public void verifySignatureException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        doReturn(publicKey).when(mSecurity).generatePublicKey(anyString());
        doThrow(new SignatureException()).when(mSecurity).verify(logger, publicKey, signedData, signature);

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        verify(mSecurity).verify(logger, publicKey, signedData, signature);
    }

    @Test
    public void verifyIllegalArgumentException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        doReturn(publicKey).when(mSecurity).generatePublicKey(anyString());
        doThrow(new IllegalArgumentException()).when(mSecurity).verify(logger, publicKey, signedData, signature);

        assertThat(mSecurity.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        verify(mSecurity).verify(logger, publicKey, signedData, signature);
    }
}