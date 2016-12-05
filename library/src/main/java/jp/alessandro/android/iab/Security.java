/* Copyright (c) 2012 Google Inc.
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
 */

package jp.alessandro.android.iab;

import android.text.TextUtils;
import android.util.Base64;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import jp.alessandro.android.iab.logger.Logger;

/**
 * Security-related methods. For a secure implementation, all of this code
 * should be implemented on a server that communicates with the
 * application on the device. For the sake of simplicity and clarity of this
 * example, this code is included here and is executed on the device. If you
 * must verify the purchases on the phone, you should obfuscate this code to
 * make it harder for an attacker to replace the code with stubs that treat all
 * purchases as verified.
 */
public class Security {

    private static final String KEY_FACTORY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    private Security() {
    }

    /**
     * Verifies that the data was signed with the given signature, and returns
     * the verified purchase. The data is in JSON format and signed
     * with a private key. The data also contains the purchase state
     * and product ID of the purchase.
     *
     * @param purchaseData    the purchase data used for debug validation.
     * @param logger          the logger to use for printing events
     * @param base64PublicKey the base64-encoded public key to use for verifying.
     * @param signedData      the signed JSON string (signed, not encrypted)
     * @param signature       the signature for the data, signed with the private key
     */
    public static boolean verifyPurchase(String purchaseData, Logger logger, String base64PublicKey,
                                         String signedData, String signature) {

        if (TextUtils.isEmpty(signedData) || TextUtils.isEmpty(base64PublicKey) ||
                TextUtils.isEmpty(signature)) {

            if (isTestingStaticResponse(purchaseData)) {
                logger.e(Logger.TAG, String.format("Testing static response: %s", purchaseData));
                return true;
            }
            logger.e(Logger.TAG, "Purchase verification failed: missing data.");
            return false;
        }
        PublicKey key = Security.generatePublicKey(logger, base64PublicKey);
        return Security.verify(logger, key, signedData, signature);
    }

    /**
     * In case of tests it will return true because test purchases doesn't have a signature
     * See https://developer.android.com/google/play/billing/billing_testing.html
     *
     * @param purchaseData the data used to purchase
     */
    private static boolean isTestingStaticResponse(String purchaseData) {
        if (BuildConfig.DEBUG &&
                (purchaseData.equals("android.test.purchased")
                        || purchaseData.equals("android.test.canceled")
                        || purchaseData.equals("android.test.refunded")
                        || purchaseData.equals("android.test.item_unavailable"))) {
            return true;
        }
        return false;
    }

    /**
     * Generates a PublicKey instance from a string containing the
     * Base64-encoded public key.
     *
     * @param logger           the logger to use for printing events
     * @param encodedPublicKey Base64-encoded public key
     * @throws IllegalArgumentException if encodedPublicKey is invalid
     */
    private static PublicKey generatePublicKey(Logger logger, String encodedPublicKey) {
        try {
            byte[] decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
            return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            logger.e(Logger.TAG, "Invalid key specification.");
            throw new IllegalArgumentException(e);
        } catch (IllegalArgumentException e) {
            logger.e(Logger.TAG, "Base64 decoding failed.");
            throw e;
        }
    }

    /**
     * Verifies that the signature from the server matches the computed
     * signature on the data.  Returns true if the data is correctly signed.
     *
     * @param logger     the logger to use for printing events
     * @param publicKey  public key associated with the developer account
     * @param signedData signed data from server
     * @param signature  server signature
     * @return true if the data and signature match
     */
    private static boolean verify(Logger logger, PublicKey publicKey,
                                  String signedData, String signature) {
        Signature sig;
        try {
            sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(signedData.getBytes());
            if (!sig.verify(Base64.decode(signature, Base64.DEFAULT))) {
                logger.e(Logger.TAG, "Signature verification failed.");
                return false;
            }
            return true;
        } catch (NoSuchAlgorithmException e) {
            logger.e(Logger.TAG, "NoSuchAlgorithmException.");
        } catch (InvalidKeyException e) {
            logger.e(Logger.TAG, "Invalid key specification.");
        } catch (SignatureException e) {
            logger.e(Logger.TAG, "Signature exception.");
        } catch (IllegalArgumentException e) {
            logger.e(Logger.TAG, "Base64 decoding failed.");
        }
        return false;
    }
}