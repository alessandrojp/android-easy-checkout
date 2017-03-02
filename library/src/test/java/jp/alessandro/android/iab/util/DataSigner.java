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

package jp.alessandro.android.iab.util;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Locale;

import jp.alessandro.android.iab.logger.Logger;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/26.
 */

public class DataSigner {

    @SuppressWarnings("checkstyle:linelength")
    private static final String PRIVATE_KEY_BASE_64_ENCODED = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDtIS1XtZPW8kp1LV8GCyRiT5zyPphRrqTPw3AtsPsSQoaH7ShxKax17gF7CtAOKMcLTPoLGzezwqSzYkLvk1NlS9FBE3lPX0+jajBNdOuRPn5mHae3n/SWPtGczIHqpgx5V5sOHihSaPhiQt1DdCM6kuMZ6nXGMi6c68mukyI2RC5GXcQ0FuTARsMrNKq5dcyeCN+THY/Id+KtsTZ0NVeQbzkbnjYpIF84cXUBCkQ7uGJGDPxvklO5J7ig51hzXVYbhs3GculxR6HzHAT23FyKkOvPpxQV9voPeUvzM8jPJnGLAivT4bQ7uKx768gxo/Qk4Dz3V6qu4FUQjtY8LJPRAgMBAAECggEATUdYrZLhYVWI6nMk2qVa8Ccd8Nxxa31M/OCmeF2LFUJU8YtaeLaqG6y7EsxNTbAAXjBx9JikKJMwdb16LvWGYia5RUoBaNqY65q5rySBeM4zBzh25iLc5PIIAd+sHzqKKilgwNMXNPQ8rlk4HrmEmZwxIssEItlL05wMGDafGaux8OVBlLqRMIGAQjaKjGc66SgFxkiiiolUlQRcvm7szXC/wXi28f7JNImFXeH5FwhHB41fbHF7eHci2/9PRCTI6pawiiSVJqj3g0A7TNuYXSB9AtZdHX1iOr72N33P/MvWwnapGXkKDm6TX+my6XTQY0qZc1MtPlEuWKMUWsgweQKBgQD/DhNkBhaY8DpOflgksmJFumG2po8CK9eGQreUs/NoE1nKxItQAVLjohVd8+aoTuiG2IUCX9Pe5OYOAOjNQ4owvFx5KBty6lhGXaOOrRUbfRtn3PYTgDsc+n75AIkn6UyabaDEIY8EmyC8wr3PX/fEod5vf1J+mKSMLn13gj1KXwKBgQDuAhlMeYMXA0sJyUhwCKMa6dnBEoxKNjHDclLDfpPVf47ogA+P2MTvKnOn7EfwfLmiU/KqbYM+8KgJRyaofMyWvoIB873PI0G/l/d8DW3rMv1K8zPLrgknUpKDMt0rFzxlSm5tYFwvSTseOUZLPvEJLcYUKfuf2uWk82gdI8ovzwKBgFBYeclHlbTF8Egrys58lzKJ/SARpfk0IGe9+qDQczv05JNYiN5CHH9y3rJDFAUvHlbkPDo8P7z2dHYy2SNYRF8H50WPWd5AbmB0PQLECWMobQqx856/BWAilP8RqSM2fhgjssI2JBx6VbzAyBRckeuSZkTPYghZQ3SZbJLKJ06XAoGAJy6XRZy3dQFoyAqn7zGs0FBxNbS8/bagSKG4eFCNO9eNCj+S0EaKXSkq8xkV2sRdtxiE2YO/2Iu7zhM1jQVGlQZ11qZut/wA5e65omV/k/nH8x/Ihh53iU6xqgGkoWRo3+/57+2uH2a54cbiCJ8rBSzQ8B7dOrrJlXcwy6NJtMcCgYAdM7gR+aVFXsedq1QEXvpnggua70VPu56xHJ8GCh1zrDu9UubkZQ9bB74kNakzvhGBmLRs+Grp6wLIm66C4MgmlUbxDnOWQLkmHvBDVn9z60RE/MTxADLqlGWDkuUpSZHN1WSfKlRpj/VeLVpAREWYBSXqjWZA5sD/GKG8l6OTJg==";

    /**
     * Sign some data for testing
     *
     * @param signedData
     * @return
     */
    public String sign(String signedData, String keyFactoryAlgorithm, String signatureAlgorithm) {
        String baseEncodedSign = null;
        try {
            byte[] decodedPrivateKey = Base64.decode(PRIVATE_KEY_BASE_64_ENCODED.getBytes("UTF-8"), Base64.DEFAULT);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedPrivateKey);
            KeyFactory kf = KeyFactory.getInstance(keyFactoryAlgorithm);
            PrivateKey privateKey = kf.generatePrivate(spec);

            Signature sig = Signature.getInstance(signatureAlgorithm);
            sig.initSign(privateKey);
            sig.update(signedData.getBytes("UTF-8"));
            baseEncodedSign = Base64.encodeToString(sig.sign(), Base64.DEFAULT);

            Log.d(Logger.TAG, String.format(Locale.ENGLISH, "BaseEncodedSign: %s", baseEncodedSign));

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return baseEncodedSign;
    }
}