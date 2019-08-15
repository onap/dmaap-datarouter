/**
 * -
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dmaap.datarouter.provisioning.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * The Processing of a Password.  Password can be encrypted and decrypted.
 * @author Vikram Singh
 * @version $Id: PasswordProcessor.java,v 1.0 2016/12/14 10:16:52 EST
 */
public class PasswordProcessor {

    private static final String SECRET_KEY_FACTORY_TYPE = "PBEWithMD5AndDES";
    private static final String PASSWORD_ENCRYPTION_STRING =
            (new DB()).getProperties().getProperty("org.onap.dmaap.datarouter.provserver.passwordencryption");
    private static final char[] PASSWORD = PASSWORD_ENCRYPTION_STRING.toCharArray();
    private static final byte[] SALT = {(byte) 0xde, (byte) 0x33, (byte) 0x10,
        (byte) 0x12, (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,};

    private PasswordProcessor(){
    }

    /**
     * Encrypt password.
     * @param property the Password
     * @return Encrypted password.
     */
    public static String encrypt(String property) throws GeneralSecurityException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_TYPE);
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance(SECRET_KEY_FACTORY_TYPE);
        pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 32));
        return Base64.getEncoder().encodeToString(pbeCipher.doFinal(property.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Decrypt password.
     * @param property the Password
     * @return Decrypt password.
     */
    public static String decrypt(String property) throws GeneralSecurityException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_TYPE);
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance(SECRET_KEY_FACTORY_TYPE);
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 32));
        return new String(pbeCipher.doFinal(Base64.getDecoder().decode(property)), StandardCharsets.UTF_8);
    }

}
