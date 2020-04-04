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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.dmaap.datarouter.provisioning.InternalServlet;
import org.onap.dmaap.datarouter.provisioning.beans.Parameters;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.provisioning.beans.Parameters")
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class LogfileLoaderTest {

    private static EntityManagerFactory emf;
    private static EntityManager em;
    private LogfileLoader lfl = LogfileLoader.getLoader();
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
        InternalServlet internalServlet = new InternalServlet();
    }

    @AfterClass
    public static void tearDownClass() {
        em.clear();
        em.close();
        emf.close();
    }

    @Test
    public void Verify_File_Processing_Returns_Expected_Array() {
        int[] actual = lfl.process(testLog);
        int[] expect = {5, 7};
        Assert.assertArrayEquals(expect, actual);
        Assert.assertNotNull(lfl.getBitSet());
        Assert.assertTrue(lfl.isIdle());
    }

    @Test
    public void Verify_Records_Prune_When_Record_Count_Is_Less_Then_Threshold() {
        lfl.process(testLog);
        PowerMockito.mockStatic(Parameters.class);
        PowerMockito.when(Parameters.getParameter(Parameters.PROV_LOG_RETENTION)).thenReturn(new Parameters(Parameters.PROV_LOG_RETENTION, "0"));
        PowerMockito.when(Parameters.getParameter(Parameters.DEFAULT_LOG_RETENTION)).thenReturn(new Parameters(Parameters.DEFAULT_LOG_RETENTION, "1000000"));
        assertFalse(lfl.pruneRecords());
    }

    @Test
    public void Verify_Records_Prune_When_Record_Count_Is_Greater_Then_Threshold() {
        lfl.process(testLog);
        PowerMockito.mockStatic(Parameters.class);
        PowerMockito.when(Parameters.getParameter(Parameters.PROV_LOG_RETENTION)).thenReturn(new Parameters(Parameters.PROV_LOG_RETENTION, "0"));
        PowerMockito.when(Parameters.getParameter(Parameters.DEFAULT_LOG_RETENTION)).thenReturn(new Parameters(Parameters.DEFAULT_LOG_RETENTION, "1"));
        assertTrue(lfl.pruneRecords());
    }


    private void prepFile(File logFile) {
        String testLogs =           "2018-08-29-10-10-10-543.|LOG|1|1|https://dmaap-dr-prov:/url/file123|POST|application/vnd.att-dr.feed|100|mockType|file123|https://dmaap-dr-prov|user123|200|1|1|200|2|2\n"
                                  + "2018-08-29-10-10-10-543.|EXP|1|1|1|'url/file123'|PUT|null|3|new reason|4\n"
                                  + "2018-08-29-10-10-10-543.|PUB|1|1|https://dmaap-dr-prov:8443/publish/1/file123/|POST|application/vnd.att-dr.feed|2|128.0.0.9|user123|200\n"
                                  + "2018-08-29-10-10-10-543.|PBF|1|1|https://dmaap-dr-prov:8443/publish/1/file123/|POST|application/vnd.att-dr.feed|100|100|128.0.0.9|user123|failed\n"
                                  + "2018-08-29-10-10-10-543.|DLX|1|1|1|100|100\n"
                                  + "2018-08-29-10-10-10-543.|Bad Record|||\n"
                                  + "2018-08-29-10-10-10-543.|DEL|2|1|2|https://dmaap-dr-prov:8443/publish/1/file123/|PUT|application/vnd.att-dr.feed|100|user123|200|123456";
        try (FileWriter fileWriter = new FileWriter(logFile)) {
            fileWriter.write(testLogs);
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
}
