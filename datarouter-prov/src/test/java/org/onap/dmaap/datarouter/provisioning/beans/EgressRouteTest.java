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
import org.onap.dmaap.datarouter.provisioning.utils.DB;
import org.onap.dmaap.datarouter.provisioning.utils.DataSource;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class EgressRouteTest {

    private EgressRoute egressRoute;

    private static EntityManagerFactory emf;
    private static EntityManager em;
    private DB db;
    private DataSource ds;

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
        db = new DB();
        egressRoute = new EgressRoute(2, 1);
    }

    @Test
    public void Verify_EgressRoute_Is_Removed_Successfully() throws SQLException, ClassNotFoundException {
        Assert.assertEquals(1, EgressRoute.getAllEgressRoutes().size());
        EgressRoute egressRoute = new EgressRoute(1, 1);
        egressRoute.doDelete(DataSource.getConnection());
        Assert.assertEquals(0, EgressRoute.getAllEgressRoutes().size());
    }

    @Test
    public void Verify_EgressRoute_Is_Updated_Successfully() throws SQLException {
        EgressRoute egressRoute = new EgressRoute(1, 1);
        EgressRoute egressRoute1 = new EgressRoute(1, 1);
        Assert.assertEquals(egressRoute.hashCode(), egressRoute1.hashCode());
        Assert.assertEquals(egressRoute, egressRoute1);
        Assert.assertEquals(egressRoute.toString(), egressRoute1.toString());
    }
}