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
import java.util.Properties;

/**
 * Load the DB JDBC driver, and manage a simple pool of connections to the DB.
 *
 * @author Robert Eby
 * @version $Id$
 */
public class DB {

//    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
//
//    public String dbUrl;
//    public String dbLogin;
//    public String dbPassword;
//    public String dbDriver;
//    private static Properties props;
//    private static String httpsPort;
//    private static String httpPort;
//
//
//    public DB() {
//        init();
//    }
//
//    private static DB db = new DB();
//
//    public static DB getInstance() {
//        return db;
//    }
//
//    /**
//     * Construct a DB object.  If this is the very first creation of this object, it will load a copy of the properties
//     * for the server, and attempt to load the JDBC driver for the database. If a fatal error occurs (e.g. either the
//     * properties file or the DB driver is missing), the JVM will exit.
//     */
//    private void init() {
//        if (props == null) {
//            props = new Properties();
//            try {
//                props.load(new FileInputStream(getProperty("org.onap.dmaap.datarouter.provserver.properties",
//                        "/opt/app/datartr/etc/provserver.properties")));
//                dbUrl = (String) props.get("org.onap.dmaap.datarouter.db.url");
//                dbLogin = (String) props.get("org.onap.dmaap.datarouter.db.login");
//                dbPassword = (String) props.get("org.onap.dmaap.datarouter.db.password");
//                httpsPort = (String) props.get("org.onap.dmaap.datarouter.provserver.https.port");
//                httpPort = (String) props.get("org.onap.dmaap.datarouter.provserver.http.port");
//                dbDriver = (String) props.get("org.onap.dmaap.datarouter.db.driver");
//                Class.forName(dbDriver);
//            } catch (IOException e) {
//                intlogger.error("PROV9003 Opening properties: " + e.getMessage(), e);
//                exit(1);
//            } catch (ClassNotFoundException e) {
//                intlogger.error("PROV9004 cannot find the DB driver: " + e);
//                exit(1);
//            }
//        }
//    }
//
//    /**
//     * Get the provisioning server properties (loaded from provserver.properties).
//     *
//     * @return the Properties object
//     */
//    public Properties getProperties() {
//        return props;
//    }
//
//
//
//
//    public static String getHttpsPort() {
//        return httpsPort;
//    }
//
//    public static String getHttpPort() {
//        return httpPort;
//    }




}
