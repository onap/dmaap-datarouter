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

import static java.lang.System.exit;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class DbItialization {

    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");

    public DbItialization() {
    }

    /**
     * Run all necessary retrofits required to bring the database up to the level required for this version of the
     * provisioning server.  This should be run before the server itself is started.
     *
     * @return true if all retrofits worked, false otherwise
     */
    public boolean runRetroFits() {
        return retroFit1();
    }

    /**
     * Retrofit 1 - Make sure the expected tables are in DB and are initialized. Uses sql_init_01.sql to setup the DB.
     *
     * @return true if the retrofit worked, false otherwise
     */
    private boolean retroFit1() {
        final String[] expectedTables =
                {"FEEDS", "FEED_ENDPOINT_ADDRS", "FEED_ENDPOINT_IDS", "PARAMETERS", "SUBSCRIPTIONS", "LOG_RECORDS",
                        "INGRESS_ROUTES", "EGRESS_ROUTES", "NETWORK_ROUTES", "NODESETS", "NODES", "GROUPS"};
        Connection connection = null;
        try {
            connection = DataSource.getConnection();
            Set<String> actualTables = getTableSet(connection);
            boolean initialize = false;
            for (String tableName : expectedTables) {
                initialize |= !actualTables.contains(tableName);
            }
            if (initialize) {
                intlogger.info("PROV9001: First time startup; The database is being initialized.");
                runInitScript(connection, 1);
            }
        } catch (ClassNotFoundException cnfe) {
            intlogger.error(cnfe.getMessage());
        } catch (SQLException e) {
            intlogger.error("PROV9000: The database credentials are not working: " + e.getMessage(), e);
            return false;
        } finally {
            if (connection != null) {
                DataSource.returnConnection(connection);
            }
        }
        return true;
    }

    /**
     * Get a set of all table names in the DB.
     *
     * @param connection a DB connection
     * @return the set of table names
     */
    private Set<String> getTableSet(Connection connection) {
        Set<String> tables = new HashSet<>();
        try {
            DatabaseMetaData md = connection.getMetaData();
            ResultSet rs = md.getTables(null, null, "%", null);
            if (rs != null) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME").toUpperCase());
                }
                rs.close();
            }
        } catch (SQLException e) {
            intlogger.error("PROV9010: Failed to get TABLE data from DB: " + e.getMessage(), e);
        }
        return tables;
    }

    /**
     * Initialize the tables by running the initialization scripts located in the directory specified by the property
     * <i>org.onap.dmaap.datarouter.provserver.dbscripts</i>.  Scripts have names of the form
     * sql_init_NN.sql
     *
     * @param connection a DB connection
     * @param scriptId   the number of the sql_init_NN.sql script to run
     */
    private void runInitScript(Connection connection, int scriptId) {
        Properties props = DbConnectionPool.getProperties();
        String scriptDir = (String) props.get("org.onap.dmaap.datarouter.provserver.dbscripts");
        String scriptFile = String.format("%s/sql_init_%02d.sql", scriptDir, scriptId);
        if (!(new File(scriptFile)).exists()) {
            intlogger.error("PROV9005 Failed to load sql script from : " + scriptFile);
            exit(1);
        }
        try (LineNumberReader lineReader = new LineNumberReader(new FileReader(scriptFile));
             Statement statement = connection.createStatement()) {
            StringBuilder strBuilder = new StringBuilder();
            String line;
            while ((line = lineReader.readLine()) != null) {
                if (!line.startsWith("--")) {
                    line = line.trim();
                    strBuilder.append(line);
                    executeDdlStatement(statement, strBuilder, line);
                }
            }
            strBuilder.setLength(0);
        } catch (Exception e) {
            intlogger.error("PROV9002 Error when initializing table: " + e.getMessage(), e);
            exit(1);
        }
    }

    private void executeDdlStatement(Statement statement, StringBuilder strBuilder, String line) throws SQLException {
        if (line.endsWith(";")) {
            // Execute one DDL statement
            String sql = strBuilder.toString();
            strBuilder.setLength(0);
            statement.execute(sql);
        }
    }
}
