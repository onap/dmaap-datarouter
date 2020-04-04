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

package org.onap.dmaap.datarouter.provisioning.beans;

import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.dmaap.datarouter.provisioning.utils.ProvDbUtils;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class NetworkRouteTest {

    private NetworkRoute networkRoute = new NetworkRoute("node01.","node03.","node02.");
    private ProvDbUtils provDbUtils = ProvDbUtils.getInstance();


    private static EntityManagerFactory emf;
    private static EntityManager em;

    @BeforeClass
    public static void init() {
        emf = Persistence.createEntityManagerFactory("dr-unit-tests");
        em = emf.createEntityManager();
        System.setProperty(
                "org.onap.dmaap.datarouter.provserver.properties",
                "src/test/resources/h2Database.properties");
    }

    @AfterClass
    public static void tearDownClass() {
        em.clear();
        em.close();
        emf.close();
    }
    @Before
    public void setUp() throws Exception {
        try (Connection conn = provDbUtils.getConnection()) {
            networkRoute.doInsert(conn);
        }
    }

    @Test
    public void Verify_NetworkRoute_Is_Removed_Successfully() throws SQLException {
        Assert.assertEquals(2, NetworkRoute.getAllNetworkRoutes().size());
        networkRoute.doDelete(provDbUtils.getConnection());
        Assert.assertEquals(1, NetworkRoute.getAllNetworkRoutes().size());
    }

    @Test
    public void Verify_NetworkRoute_Is_Updated_Successfully() throws SQLException {
        NetworkRoute networkRoute = new NetworkRoute("stub_from.", "stub_to.", "node02.");
        networkRoute.doUpdate(provDbUtils.getConnection());
        for (NetworkRoute net :
            NetworkRoute.getAllNetworkRoutes()) {
            Assert.assertEquals(5, net.getVianode());
        }
        NetworkRoute networkRoute1 = new NetworkRoute("stub_from.", "stub_to.", "node02.");
        Assert.assertEquals(networkRoute.hashCode(), networkRoute1.hashCode());
        Assert.assertEquals(networkRoute, networkRoute1);
        Assert.assertEquals(networkRoute.toString(), networkRoute1.toString());
    }
}