/*******************************************************************************
 * ============LICENSE_START==================================================
 * * org.onap.dmaap
 * * ===========================================================================
 * * Copyright © 2019 Nordix Foundation.
 * * ===========================================================================
 * * Licensed under the Apache License, Version 2.0 (the "License");
 * * you may not use this file except in compliance with the License.
 * * You may obtain a copy of the License at
 * *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 * *
 *  * Unless required by applicable law or agreed to in writing, software
 * * distributed under the License is distributed on an "AS IS" BASIS,
 * * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * * See the License for the specific language governing permissions and
 * * limitations under the License.
 * * ============LICENSE_END====================================================
 * *
 * * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 * *
 ******************************************************************************/

package org.onap.dmaap.datarouter.provisioning.utils;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.onap.dmaap.datarouter.provisioning.InternalServlet;
import org.onap.dmaap.datarouter.provisioning.beans.Parameters;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;


@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.provisioning.beans.Parameters")
public class LogfileLoaderTest {

    private static EntityManagerFactory emf;
    private static EntityManager em;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

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
    public void Verify_File_Processing_when_Req_Type_LOG() throws IOException {
        LogfileLoader lfl = LogfileLoader.getLoader();
        int[] actual = lfl.process(Prep_File_Type_Log());
        int[] expect = {0, 1};
        Assert.assertArrayEquals(expect, actual);
    }


    @Test
    public void Verify_File_Processing_when_Req_Type_EXP() throws IOException {
        LogfileLoader lfl = LogfileLoader.getLoader();
        int[] actual = lfl.process(Prep_File_Type_Exp());
        int[] expect = {0, 1};
        Assert.assertArrayEquals(expect, actual);
    }


    @Test
    public void Verify_Records_Prune_When_Record_Count_Is_Less_Then_Threshold() throws IOException{
        LogfileLoader lfl = LogfileLoader.getLoader();
        lfl.process(Prep_File_Type_Pub());
        PowerMockito.mockStatic(Parameters.class);
        PowerMockito.when(Parameters.getParameter(Parameters.PROV_LOG_RETENTION)).thenReturn(new Parameters(Parameters.PROV_LOG_RETENTION, "0"));
        Assert.assertEquals(lfl.pruneRecords(), false);
    }


    @Test
    public void Verify_Histogram_When_Request_Type_Post() throws Exception {
        LogfileLoader lfl = LogfileLoader.getLoader();
        lfl.process(Prep_File_Type_Pub());

        Map<Long, Long> expect = new HashMap<>();
        expect.put(17772L,2L);
        expect.put(29353L,1L);
        Map<Long, Long> actual = lfl.getHistogram();
        assertTrue(actual.equals(expect));
    }


    public File Prep_File_Type_Exp() throws IOException{
        File file1 = folder.newFile("myfile1.txt");
        String fileContent = "2018-08-29-10-10-10-543.|EXP|1|1|1|'url/file123'|method|ctype|3|other|4";
        try (FileWriter fileWriter = new FileWriter(file1)) {
            fileWriter.write(fileContent);
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
        return file1;
    }


    public File Prep_File_Type_Pub() throws IOException{
        File file1 = folder.newFile("myfile1.txt");
        String fileContent = "2018-08-29-10-10-10-543.|PUB|1|1|https://dmaap-dr-prov:8443/publish/1/file123/|POST|application/vnd.att-dr.feed|2|128.0.0.9|user123|200";

        try (FileWriter fileWriter = new FileWriter(file1)) {
            fileWriter.write(fileContent);
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
        return file1;
    }


    public File Prep_File_Type_Log() throws IOException{
        File file1 = folder.newFile("myfile1.txt");
        String fileContent = "2018-08-29-10-10-10-543.|LOG|1|1|url/file123|method|1|1|type|1|128.0.0.9|user123|2|1|1|1|other|1";
        try (FileWriter fileWriter = new FileWriter(file1)) {
            fileWriter.write(fileContent);
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
        return file1;
    }
}
