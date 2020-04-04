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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class PurgeLogDirTaskTest {

    private static EntityManagerFactory emf;
    private static EntityManager em;
    private PurgeLogDirTask purgeLogDirTask = new PurgeLogDirTask();
    private File testLog;

    @Before
    public void setUp() throws Exception {
        testLog = new File(System.getProperty("user.dir") + "/src/test/resources/IN.test_prov_logs");
        prepFile(testLog);
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(testLog.toPath());
    }

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

    @Test
    public void Verify_Logs_Are_Purged() {
        purgeLogDirTask.run();
    }

    private void prepFile(File logFile) {
        try (FileWriter fileWriter = new FileWriter(logFile)) {
            fileWriter.write("2018-08-29-10-10-10-543.|LOG|1|1|https://dmaap-dr-prov:/url/file123|POST|application/vnd.att-dr.feed|100|mockType|file123|https://dmaap-dr-prov|user123|200|1|1|200|2|2\n");
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
}
