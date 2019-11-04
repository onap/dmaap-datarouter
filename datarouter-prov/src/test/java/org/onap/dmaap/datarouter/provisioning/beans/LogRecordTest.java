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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.utils.ProvDbUtils;
import org.onap.dmaap.datarouter.provisioning.utils.RLEBitSet;

public class LogRecordTest {

    private static EntityManagerFactory emf;
    private static EntityManager em;
    private static ProvDbUtils provDbUtils;

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
    public void setUp() throws ParseException, SQLException {
        provDbUtils = ProvDbUtils.getInstance();
    }

    @Test
    public void Validate_Load_For_Pub_Type_Sets_Correct_Values() throws SQLException, ParseException {
        setArgsLoadAndAssertEquals("pub", "{1: 'pub', 3: 'ID', 4: 1, 5: 'URL/file123', 6: 'PUT', 7: 'application/vnd.dmaap-.log-list; version=1.0', 8: 100, 9: '1', 10: '172.0.0.8', 11: 'user', 12: 204, 13: NULL, 14: NULL, 15: NULL, 16: NULL, 17: NULL, 18: 1, 19: NULL, 20: 'file123'}");
    }

    @Test
    public void Validate_Load_For_Del_Type_Sets_Correct_Values() throws SQLException, ParseException {
        setArgsLoadAndAssertEquals("del", "{1: 'del', 3: 'ID', 4: 1, 5: 'URL/file123', 6: 'PUT', 7: 'application/vnd.dmaap-.log-list; version=1.0', 8: 100, 9: NULL, 10: NULL, 11: 'user', 12: NULL, 13: 1, 14: '1', 15: 204, 16: NULL, 17: NULL, 18: 1, 19: NULL, 20: 'file123'}");
    }

    @Test
    public void Validate_Load_For_Exp_Type_Sets_Correct_Values() throws SQLException, ParseException {
        setArgsLoadAndAssertEquals("exp", "{1: 'exp', 3: 'ID', 4: 1, 5: 'URL/file123', 6: 'PUT', 7: 'application/vnd.dmaap-.log-list; version=1.0', 8: 100, 9: NULL, 10: NULL, 11: NULL, 12: NULL, 13: 1, 14: '1', 15: NULL, 16: 0, 17: 'other', 18: 1, 19: NULL, 20: 'file123'}");
    }

    @Test
    public void Validate_Load_For_Pbf_Type_Sets_Correct_Values() throws SQLException, ParseException {
        setArgsLoadAndAssertEquals("pbf", "{1: 'pbf', 3: 'ID', 4: 1, 5: 'URL/file123', 6: 'PUT', 7: 'application/vnd.dmaap-.log-list; version=1.0', 8: 100, 9: '1', 10: '172.0.0.8', 11: 'user', 12: NULL, 13: NULL, 14: NULL, 15: NULL, 16: NULL, 17: NULL, 18: 1, 19: 100, 20: 'file123'}");
    }

    @Test
    public void Validate_Load_For_Dlx_Type_Sets_Correct_Values() throws SQLException, ParseException {
        setArgsLoadAndAssertEquals("dlx", "{1: 'dlx', 3: 'ID', 4: 1, 5: 'URL/file123', 6: 'PUT', 7: 'application/vnd.dmaap-.log-list; version=1.0', 8: 100, 9: NULL, 10: NULL, 11: NULL, 12: NULL, 13: 1, 14: NULL, 15: NULL, 16: NULL, 17: NULL, 18: 1, 19: 100, 20: 'file123'}");
    }

    @Test
    public void Validate_printLogRecords_Prints_Correct_Values() throws IOException {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        String[] rlebitset = {"0-1,2-2"};
        LogRecord.printLogRecords(System.out, new RLEBitSet(rlebitset[0]));
        Assert.assertEquals("LOG|ID|1|URL/file123|PUT|application/vnd.dmaap-dr.log-list; version=1.0|100|pub|1|172.0.0.8|user|204|1|1|204|0|other|1|0\n", outContent.toString().substring(25));
    }

    private void setArgsLoadAndAssertEquals(String type, String s) throws ParseException, SQLException {
        String[] args = {"2018-08-29-10-10-10-543.", "LOG", "ID", "1", "URL/file123", "PUT", "application/vnd.dmaap-.log-list; version=1.0", "100", type, "1", "172.0.0.8", "user", "204", "1", "1", "204", "0", "other", "1", "100", "file123"};
        LogRecord logRecord = new LogRecord(args);
        String compare_string;
        try (Connection conn = provDbUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(
            "insert into LOG_RECORDS values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            logRecord.load(ps);
            compare_string = ps.toString().substring(ps.toString().indexOf("{1:"), ps.toString().indexOf(", 2:")) + ps.toString().substring(ps.toString().indexOf(", 3:"));
        }
        Assert.assertEquals(s, compare_string);
    }
}
