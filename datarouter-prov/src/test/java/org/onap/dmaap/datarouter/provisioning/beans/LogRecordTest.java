/*******************************************************************************
 * ============LICENSE_START==================================================
 * * org.onap.dmaap
 * * ===========================================================================
 * * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
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

package org.onap.dmaap.datarouter.provisioning.beans;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.utils.DB;
import org.onap.dmaap.datarouter.provisioning.utils.RLEBitSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;

public class LogRecordTest {

    private LogRecord logRecord;
    private static EntityManagerFactory emf;
    private static EntityManager em;
    private DB db;
    public static final String INSERT_SQL = "insert into LOG_RECORDS values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    PreparedStatement ps;

    @BeforeClass
    public static void init() {
        emf = Persistence.createEntityManagerFactory("dr-unit-tests");
        em = emf.createEntityManager();
        System.setProperty(
                "org.onap.dmaap.datarouter.provserver.properties",
                "src/test/resources/h2Database.properties");
    }

    @Before
    public void setUp() throws ParseException, SQLException {
        db = new DB();
        String[] args = {"2018-08-29-10-10-10-543.", "LOG","ID","1","URL/file123","PUT","application/vnd.dmaap-.log-list; version=1.0","100","pub", "1","172.0.0.8","user", "204","1","1","204","0","other","1","100","file123"};
        Connection conn = db.getConnection();
        ps = conn.prepareStatement(INSERT_SQL);
        logRecord = new LogRecord(args);
    }

    @Test
    public void Validate_load_for_pub_type_sets_correct_values() throws SQLException {
        logRecord.load(ps);
        Assert.assertEquals(ps.toString().substring(128), "3: 'ID', 4: 1, 5: 'URL/file123', 6: 'PUT', 7: 'application/vnd.dmaap-.log-list; version=1.0', 8: 100, 9: '1', 10: '172.0.0.8', 11: 'user', 12: 204, 13: NULL, 14: NULL, 15: NULL, 16: NULL, 17: NULL, 18: 1, 19: NULL, 20: 'file123'}");
    }

    @Test
    public void Validate_load_for_del_type_sets_correct_values() throws SQLException, ParseException {
        setArgsLoadAndAssertEquals("del", "3: 'ID', 4: 1, 5: 'URL/file123', 6: 'PUT', 7: 'application/vnd.dmaap-.log-list; version=1.0', 8: 100, 9: NULL, 10: NULL, 11: 'user', 12: NULL, 13: 1, 14: '1', 15: 204, 16: NULL, 17: NULL, 18: 1, 19: NULL, 20: 'file123'}");
    }

    private void setArgsLoadAndAssertEquals(String type, String s) throws ParseException, SQLException {
        String[] args = {"2018-08-29-10-10-10-543.", "LOG", "ID", "1", "URL/file123", "PUT", "application/vnd.dmaap-.log-list; version=1.0", "100", type, "1", "172.0.0.8", "user", "204", "1", "1", "204", "0", "other", "1", "100", "file123"};
        logRecord = new LogRecord(args);
        logRecord.load(ps);
        Assert.assertEquals(ps.toString().substring(128), s);
    }

    @Test
    public void Validate_load_for_exp_type_sets_correct_values() throws SQLException, ParseException {
        setArgsLoadAndAssertEquals("exp", "3: 'ID', 4: 1, 5: 'URL/file123', 6: 'PUT', 7: 'application/vnd.dmaap-.log-list; version=1.0', 8: 100, 9: NULL, 10: NULL, 11: NULL, 12: NULL, 13: 1, 14: '1', 15: NULL, 16: 0, 17: 'other', 18: 1, 19: NULL, 20: 'file123'}");
    }

    @Test
    public void Validate_load_for_pbf_type_sets_correct_values() throws SQLException, ParseException {
        setArgsLoadAndAssertEquals("pbf", "3: 'ID', 4: 1, 5: 'URL/file123', 6: 'PUT', 7: 'application/vnd.dmaap-.log-list; version=1.0', 8: 100, 9: '1', 10: '172.0.0.8', 11: 'user', 12: NULL, 13: NULL, 14: NULL, 15: NULL, 16: NULL, 17: NULL, 18: 1, 19: 100, 20: 'file123'}");
    }

    @Test
    public void Validate_load_for_dlx_type_sets_correct_values() throws SQLException, ParseException {
        setArgsLoadAndAssertEquals("dlx", "3: 'ID', 4: 1, 5: 'URL/file123', 6: 'PUT', 7: 'application/vnd.dmaap-.log-list; version=1.0', 8: 100, 9: NULL, 10: NULL, 11: NULL, 12: NULL, 13: 1, 14: NULL, 15: NULL, 16: NULL, 17: NULL, 18: 1, 19: 100, 20: 'file123'}");
    }

    @Test
    public void Validate_printLogRecords_prints_correct_values() throws IOException {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        String[] rlebitset = {"0-1,2-2"};
        LogRecord.printLogRecords(System.out, new RLEBitSet(rlebitset[0]));
        Assert.assertEquals(outContent.toString().substring(25), "LOG|ID|1|URL/file123|PUT|application/vnd.dmaap-dr.log-list; version=1.0|100|pub|1|172.0.0.8|user|204|1|1|204|0|other|1|0\n");
    }
}
