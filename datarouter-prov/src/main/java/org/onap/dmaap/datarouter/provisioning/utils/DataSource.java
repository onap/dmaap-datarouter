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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.sql.Connection;

public class DataSource {
    private static DbConnectionPool pool;
    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");

    static {
        try {
            pool = new DbConnectionPool();
        } catch (ClassNotFoundException e) {
            intlogger.error(e.getMessage());
        }
    }

    private DataSource(){}

    public static Connection getConnection() {
        return pool.getConnectionFromPool();
    }

    public static void returnConnection(Connection connection) {
        pool.returnConnnectionToPool(connection);
    }
}
