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

package org.onap.dmaap.datarouter.node.utils;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Properties;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import org.onap.dmaap.datarouter.node.eelf.EelfMsgs;

public class NodeTlsManager {

    private static final EELFLogger eelfLogger = EELFManager.getInstance().getLogger(NodeTlsManager.class);

    private String keyStoreType;
    private String keyStorefile;
    private String keyStorePassword;
    private String keyManagerPassword;
    private final String[] enabledProtocols;

    public NodeTlsManager(Properties properties) {
        enabledProtocols = properties.getProperty("NodeHttpsProtocols",
            "TLSv1.1|TLSv1.2").trim().split("\\|");
        setUpKeyStore(properties);
        setUpTrustStore(properties);
    }

    private void setUpKeyStore(Properties properties) {
        keyStoreType = properties.getProperty("KeyStoreType", "PKCS12");
        keyStorefile = properties.getProperty("KeyStorePath");
        keyStorePassword = properties.getProperty("KeyStorePass");
        keyManagerPassword = properties.getProperty("KeyManagerPass");
    }

    private void setUpTrustStore(Properties properties) {
        String trustStoreType = properties.getProperty("TrustStoreType", "jks");
        String trustStoreFile = properties.getProperty("TrustStorePath");
        String trustStorePassword = properties.getProperty("TrustStorePass");
        if (trustStoreFile != null && trustStoreFile.length() > 0) {
            eelfLogger.info("TrustStore found. Loading {} file {} to System Properties.", trustStoreType, trustStoreFile);
            System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
            System.setProperty("javax.net.ssl.trustStore", trustStoreFile);
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            return;
        }
        eelfLogger.error("TrustStore not found. Falling back to 1 way TLS");
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public String getKeyStorefile() {
        return keyStorefile;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getKeyManagerPassword() {
        return keyManagerPassword;
    }

    public String[] getEnabledProtocols() {
        return enabledProtocols;
    }

    /**
     * Get the CN value of the first private key entry with a certificate.
     *
     * @return CN of the certificate subject or null
     */
    public String getMyNameFromCertificate() {
        return getCanonicalName(this.keyStoreType, this.keyStorefile, this.keyStorePassword);
    }

    private String getCanonicalName(String kstype, String ksfile, String kspass) {
        KeyStore ks;
        try {
            ks = KeyStore.getInstance(kstype);
            if (loadKeyStore(ksfile, kspass, ks)) {
                return (null);
            }
        } catch (Exception e) {
            NodeUtils.setIpAndFqdnForEelf("getCanonicalName");
            eelfLogger.error(EelfMsgs.MESSAGE_KEYSTORE_LOAD_ERROR, e, ksfile);
            return (null);
        }
        return (getCanonicalName(ks));
    }

    private String getCanonicalName(KeyStore ks) {
        try {
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String name = getNameFromSubject(ks, aliases);
                if (name != null) {
                    return name;
                }
            }
        } catch (Exception e) {
            eelfLogger.error("NODE0402 Error extracting my name from my keystore file " + e);
        }
        return (null);
    }

    private boolean loadKeyStore(String ksfile, String kspass, KeyStore ks)
        throws NoSuchAlgorithmException, CertificateException {
        try (FileInputStream fileInputStream = new FileInputStream(ksfile)) {
            ks.load(fileInputStream, kspass.toCharArray());
        } catch (IOException ioException) {
            eelfLogger.error("IOException occurred while opening FileInputStream: " + ioException.getMessage(),
                ioException);
            return true;
        }
        return false;
    }

    private String getNameFromSubject(KeyStore ks, Enumeration<String> aliases) throws KeyStoreException {
        String alias = aliases.nextElement();
        String nameFromSubject = null;
        if (ks.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
            if (cert != null) {
                String subject = cert.getSubjectX500Principal().getName();
                try {
                    LdapName ln = new LdapName(subject);
                    for (Rdn rdn : ln.getRdns()) {
                        if (rdn.getType().equalsIgnoreCase("CN")) {
                            nameFromSubject = rdn.getValue().toString();
                        }
                    }
                } catch (InvalidNameException e) {
                    eelfLogger.error("No valid CN not found for dr-node cert", e);
                }
            }
        }
        return nameFromSubject;
    }
}
