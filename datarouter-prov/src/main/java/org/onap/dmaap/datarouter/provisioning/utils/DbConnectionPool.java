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

package org.onap.dmaap.datarouter.provisioning.utils;

import static java.lang.System.exit;
import static java.lang.System.getProperty;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class DbConnectionPool {
    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private static Properties props;
    private static String dbUrl;
    private static String dbLogin;
    private static String dbPassword;
    private static String dbDriver;
    private static String httpsPort;
    private static String httpPort;
    List<Connection> availableConnections = new ArrayList<>();

    public DbConnectionPool() throws ClassNotFoundException {
        initializeDB();
        initializeConnectionPool((String) props.get("org.onap.dmaap.datarouter.db.url"), (String) props.get("org.onap.dmaap.datarouter.db.login"), (String) props.get("org.onap.dmaap.datarouter.db.password"));
    }

    private  void initializeDB() {
        if (props == null) {
            props = DbUtils.getProperties();
            }
        }


    private void initializeConnectionPool(String dbUrl, String dbLogin, String dbPassword) {
        while (!checkIfConnectionPoolIsFull()) {
            availableConnections.add(createNewConnectionForPool(dbUrl, dbLogin, dbPassword));
        }
    }

    private synchronized boolean checkIfConnectionPoolIsFull() {
        final int MAX_POOL_SIZE = 100;

        if (availableConnections.size() < MAX_POOL_SIZE) {
            return false;
        }
        return true;
    }

    //Create a connection
    private Connection createNewConnectionForPool(String dbUrl, String dbLogin, String dbPassword) {
        try {
            return DriverManager.getConnection(dbUrl, dbLogin, dbPassword);
        } catch (SQLException e) {
            intlogger.error(e.getMessage());
        }
        return null;
    }

    public synchronized Connection getConnectionFromPool() {
        Connection connection = null;
        if(!availableConnections.isEmpty()) {
            connection = availableConnections.get(0);
            availableConnections.remove(0);
        }
        return connection;
    }

    public synchronized void returnConnnectionToPool(Connection connection) {
        availableConnections.add(connection);
    }



}
