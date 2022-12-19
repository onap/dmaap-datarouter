/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class ProvTlsManager {

    private static final EELFLogger eelfLogger = EELFManager.getInstance().getLogger(ProvTlsManager.class);

    private final String keyStoreType;
    private final String keyStorefile;
    private final String keyStorePassword;
    private final String keyManagerPassword;
    private KeyStore keyStore;

    private final String trustStoreType;
    private final String trustStoreFile;
    private final String trustStorePassword;
    private KeyStore trustStore;

    private final String[] enabledProtocols;

    /**
     * Utility class to handle Provisioning server SSL configuration
     *
     * @param properties DR provisioning server properties
     * @throws Exception for any unrecoverable problem
     */
    public ProvTlsManager(Properties properties, boolean preLoadCerts) throws Exception {

        keyStoreType = properties.getProperty("org.onap.dmaap.datarouter.provserver.keystoretype", "PKCS12");
        keyStorefile = properties.getProperty("org.onap.dmaap.datarouter.provserver.keystorepath");
        keyStorePassword = properties.getProperty("org.onap.dmaap.datarouter.provserver.keystorepassword");
        keyManagerPassword = properties.getProperty("org.onap.dmaap.datarouter.provserver.keymanagerpassword");

        trustStoreType = properties.getProperty("org.onap.dmaap.datarouter.provserver.truststoretype", "jks");
        trustStoreFile = properties.getProperty("org.onap.dmaap.datarouter.provserver.truststorepath");
        trustStorePassword = properties.getProperty("org.onap.dmaap.datarouter.provserver.truststorepassword");

        if (preLoadCerts) {
            eelfLogger.debug("ProvTlsManager: Attempting to pre load certificate data from config.");
            setUpKeyStore();
            setUpTrustStore();
        }

        enabledProtocols = properties.getProperty(
            "org.onap.dmaap.datarouter.provserver.https.include.protocols",
            "TLSv1.1|TLSv1.2").trim().split("\\|");
    }

    /**
     * Gets an SSLSocketFactory instance constructed using the relevant SSL properties
     *
     * @return SSLSocketFactory
     * @throws KeyStoreException if SSL config is invalid
     */
    public SSLSocketFactory getSslSocketFactory()
        throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        eelfLogger.debug("ProvTlsManager.getSslSocketFactory: Setting up SSLSocketFactory");
        if (this.trustStoreFile == null) {
            eelfLogger.warn("Warning: No trust store available.");
            return new SSLSocketFactory(this.keyStore, this.keyStorePassword);
        }
        return new SSLSocketFactory(this.keyStore, this.keyStorePassword, this.trustStore);
    }

    /**
     * Gets an SslContextFactory.Server instance constructed using the relevant SSL properties
     *
     * @return SslContextFactory.Server
     */
    public SslContextFactory.Server getSslContextFactoryServer() {
        eelfLogger.debug("ProvTlsManager.getSslContextFactoryServer: Setting up getSslContextFactoryServer");
        SslContextFactory.Server sslContextFactoryServer = new SslContextFactory.Server();
        sslContextFactoryServer.setKeyStoreType(this.keyStoreType);
        sslContextFactoryServer.setKeyStorePath(this.keyStorefile);
        sslContextFactoryServer.setKeyStorePassword(this.keyStorePassword);
        sslContextFactoryServer.setKeyManagerPassword(this.keyManagerPassword);
        if (this.trustStoreFile != null) {
            sslContextFactoryServer.setTrustStoreType(this.trustStoreType);
            sslContextFactoryServer.setTrustStorePath(this.trustStoreFile);
            sslContextFactoryServer.setTrustStorePassword(this.trustStorePassword);
        }
        sslContextFactoryServer.setIncludeProtocols(this.enabledProtocols);
        return sslContextFactoryServer;
    }

    /**
     * Get the trust store file path from dr config
     *
     * @return String
     */
    public String getTrustStoreFile() {
        return trustStoreFile;
    }

    /**
     * Get the trust store password from dr config
     *
     * @return String
     */
    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    private void setUpKeyStore()
        throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        eelfLogger.debug("ProvTlsManager.setUpKeyStore: Attempting to load keyStore {}", keyStorefile);
        keyStore = readKeyStore(keyStorefile, keyStorePassword, keyStoreType);
    }

    private void setUpTrustStore()
        throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        if (trustStoreFile != null && trustStorePassword != null) {
            eelfLogger.debug("ProvTlsManager.setUpTrustStore: Attempting to load trustStore {}", trustStoreFile);
            trustStore = readKeyStore(trustStoreFile, trustStorePassword, trustStoreType);
        } else {
            eelfLogger.warn("No truststore provided from properties. Skipping.");
        }
    }

    private KeyStore readKeyStore(String keyStore, String pass, String type)
        throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        eelfLogger.debug("ProvTlsManager.readKeyStore: Verifying load of keystore {}", keyStore);
        KeyStore ks = KeyStore.getInstance(type);
        try (FileInputStream stream = new FileInputStream(keyStore)) {
            ks.load(stream, pass.toCharArray());
        }
        return ks;
    }
}