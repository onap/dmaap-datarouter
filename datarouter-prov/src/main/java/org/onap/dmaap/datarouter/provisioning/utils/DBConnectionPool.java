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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;


public class DBConnectionPool {
    List<Connection> availableConnections = new ArrayList<>();

    public DBConnectionPool() {
        initializeConnectionPool();
    }

    private void initializeConnectionPool() {
        while(!checkIfConnectionPoolIsFull()) {
            availableConnections.add(createNewConnectionForPool());
        }
    }

    private synchronized boolean checkIfConnectionPoolIsFull() {
        final int MAX_POOL_SIZE = 100;

        if(availableConnections.size() < MAX_POOL_SIZE) {
            return false;
        }
        return true;
    }

    //Create a connection
    private Connection createNewConnectionForPool() {
        DB config = DB.getInstance();
        try {
            Class.forName(config.dbDriver);
            return DriverManager.getConnection(config.dbUrl, config.dbLogin, config.dbLogin);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
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
