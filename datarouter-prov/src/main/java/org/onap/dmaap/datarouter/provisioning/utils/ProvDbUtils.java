/*
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
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.onap.dmaap.datarouter.provisioning.ProvRunner;

public class ProvDbUtils {

    private static EELFLogger intLogger = EELFManager.getInstance().getLogger("InternalLog");
    private static DataSource dataSource;
    private static ProvDbUtils provDbUtils;

    private ProvDbUtils() {
    }

    public static ProvDbUtils getInstance() {
        if (provDbUtils == null) {
            try {
                provDbUtils = new ProvDbUtils();
                dataSource = setupDataSource(ProvRunner.getProvProperties());
            } catch (ClassNotFoundException e) {
                intLogger.error("PROV9010: Failed to load DB Driver Class: " + e.getMessage(), e);
                exit(1);
            }
        }
        return provDbUtils;
    }

    private static DataSource setupDataSource(Properties props) throws ClassNotFoundException {
        intLogger.info("PROV9009: Setting up DB dataSource");
        Class.forName((String) props.get("org.onap.dmaap.datarouter.db.driver"));
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl((String) props.get("org.onap.dmaap.datarouter.db.url"));
        dataSource.setUsername(getValue(props, "org.onap.dmaap.datarouter.db.login"));
        dataSource.setPassword(getValue(props, "org.onap.dmaap.datarouter.db.password"));
        dataSource.setMinIdle(5);
        dataSource.setMaxIdle(15);
        dataSource.setMaxOpenPreparedStatements(100);
        return dataSource;
    }

    private static String getValue(final Properties props, final String value) {
        String prop = (String) props.get(value);
        if (prop != null && prop.matches("[$][{].*[}]$")) {
            return System.getenv(prop.substring(2, prop.length() - 1));
        }
        return prop;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean initProvDB() {
        final String[] expectedTables = {
            "FEEDS", "FEED_ENDPOINT_ADDRS", "FEED_ENDPOINT_IDS", "PARAMETERS",
            "SUBSCRIPTIONS", "LOG_RECORDS", "INGRESS_ROUTES", "EGRESS_ROUTES",
            "NETWORK_ROUTES", "NODESETS", "NODES", "GROUPS"
        };
        try (Connection connection = getConnection()) {
            Set<String> actualTables = getTableSet(connection);
            boolean initialize = false;
            for (String tableName : expectedTables) {
                initialize |= !actualTables.contains(tableName);
            }
            if (initialize) {
                intLogger.info("PROV9001: First time startup; The database is being initialized.");
                runInitScript(connection, 1);
            }
        } catch (SQLException e) {
            intLogger.error("PROV9000: The database credentials are not working: " + e.getMessage(), e);
            return false;
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
            intLogger.error("PROV9010: Failed to get TABLE data from DB: " + e.getMessage(), e);
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
        String scriptDir = ProvRunner.getProvProperties().getProperty("org.onap.dmaap.datarouter.provserver.dbscripts");
        String scriptFile = String.format("%s/sql_init_%02d.sql", scriptDir, scriptId);
        if (!(new File(scriptFile)).exists()) {
            intLogger.error("PROV9005 Failed to load sql script from : " + scriptFile);
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
            intLogger.error("PROV9002 Error when initializing table: " + e.getMessage(), e);
            exit(1);
        }
    }


    private void executeDdlStatement(Statement statement, StringBuilder strBuilder, String line) throws SQLException {
        if (line.endsWith(";")) {
            String sql = strBuilder.toString();
            strBuilder.setLength(0);
            statement.execute(sql);
        }
    }
}
