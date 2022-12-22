/*-
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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.ProvRunner;
import org.onap.dmaap.datarouter.provisioning.ProvServer;

public class ProvTlsManagerTest {

    ProvTlsManager provTlsManager;

    @BeforeClass
    public static void init() {
        System.setProperty(
            "org.onap.dmaap.datarouter.provserver.properties",
            "src/test/resources/h2Database.properties");
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void Verify_Prov_Tls_Manager_Is_Configured_Correctly_And_Returns_Non_Null() throws Exception {
        provTlsManager = new ProvTlsManager(ProvRunner.getProvProperties(), true);
        Assert.assertNotNull(provTlsManager.getSslContextFactoryServer());
        Assert.assertNotNull(provTlsManager.getSslSocketFactory());
        Assert.assertEquals(provTlsManager.getTrustStoreFile(), "src/test/resources/certs/truststore.jks");
        Assert.assertEquals(provTlsManager.getTrustStorePassword(), "secret");
    }

    @Test
    public void Verify_Prov_Tls_Manager_Is_Configured_Correctly_When_Load_Certs_Is_False() throws Exception {
        provTlsManager = new ProvTlsManager(ProvRunner.getProvProperties(), false);
        Assert.assertEquals(provTlsManager.getTrustStorePassword(), "secret");
    }
}
