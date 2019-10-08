/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dmaap.datarouter.provisioning.utils;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.onap.aaf.cadi.PropAccess;

public class AafPropsUtils {

    private static AafPropsUtils aafPropsUtilsInstance = null;
    private static EELFLogger eelfLogger = EELFManager.getInstance().getLogger(AafPropsUtils.class);

    public static final String DEFAULT_TRUSTSTORE = "/opt/app/osaaf/local/org.onap.dmaap-dr.trust.jks";
    public static final String KEYSTORE_TYPE_PROPERTY = "PKCS12";
    public static final String TRUESTSTORE_TYPE_PROPERTY = "jks";
    private static final String KEYSTORE_PATH_PROPERTY = "cadi_keystore";
    private static final String KEYSTORE_PASS_PROPERTY = "cadi_keystore_password_p12";
    private static final String TRUSTSTORE_PATH_PROPERTY = "cadi_truststore";
    private static final String TRUSTSTORE_PASS_PROPERTY = "cadi_truststore_password";

    private PropAccess propAccess;

    private AafPropsUtils(File propsFile) throws IOException {
        propAccess = new PropAccess();
        try {
            propAccess.load(new FileInputStream(propsFile));
        } catch (IOException e) {
            eelfLogger.error("Failed to load props file: " + propsFile + "\n" + e.getMessage(), e);
            throw e;
        }
    }

    public static synchronized void init(File propsFile) throws IOException {
        if (aafPropsUtilsInstance != null) {
            throw new IllegalStateException("Already initialized");
        }
        aafPropsUtilsInstance = new AafPropsUtils(propsFile);
    }

    public static AafPropsUtils getInstance() {
        if (aafPropsUtilsInstance == null) {
            throw new IllegalStateException("Call AafPropsUtils.init(File propsFile) first");
        }
        return aafPropsUtilsInstance;
    }

    private String decryptedPass(String password) {
        String decryptedPass = null;
        try {
            decryptedPass = propAccess.decrypt(password, false);
        } catch (IOException e) {
            eelfLogger.error("Failed to decrypt " + password + " : " + e.getMessage(), e);
        }
        return decryptedPass;
    }

    public PropAccess getPropAccess() {
        if (propAccess == null) {
            throw new IllegalStateException("Call AafPropsUtils.init(File propsFile) first");
        }
        return propAccess;
    }

    public String getKeystorePathProperty() {
        return propAccess.getProperty(KEYSTORE_PATH_PROPERTY);
    }

    public String getKeystorePassProperty() {
        return decryptedPass(propAccess.getProperty(KEYSTORE_PASS_PROPERTY));
    }

    public String getTruststorePathProperty() {
        return propAccess.getProperty(TRUSTSTORE_PATH_PROPERTY);
    }

    public String getTruststorePassProperty() {
        return decryptedPass(propAccess.getProperty(TRUSTSTORE_PASS_PROPERTY));
    }

}
