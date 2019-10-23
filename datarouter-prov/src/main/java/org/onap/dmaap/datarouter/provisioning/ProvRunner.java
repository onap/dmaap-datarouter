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


package org.onap.dmaap.datarouter.provisioning;

import static java.lang.System.exit;
import static java.lang.System.getProperty;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import org.eclipse.jetty.server.Server;
import org.onap.dmaap.datarouter.provisioning.utils.AafPropsUtils;
import org.onap.dmaap.datarouter.provisioning.utils.LogfileLoader;
import org.onap.dmaap.datarouter.provisioning.utils.Poker;
import org.onap.dmaap.datarouter.provisioning.utils.ProvDbUtils;
import org.onap.dmaap.datarouter.provisioning.utils.PurgeLogDirTask;
import org.onap.dmaap.datarouter.provisioning.utils.SynchronizerTask;

/**
 * <p>
 * A main class which may be used to start the provisioning server with an "embedded" Jetty server. Configuration is
 * done via the properties file <i>provserver.properties</i>, which should be in the CLASSPATH. The provisioning server
 * may also be packaged with a web.xml and started as a traditional webapp.
 * </p>
 * <p>
 * Most of the work of the provisioning server is carried out within the eight servlets (configured below) that are used
 * to handle each of the eight types of requests the server may receive. In addition, there are background threads
 * started to perform other tasks:
 * </p>
 * <ul>
 * <li>One background Thread runs the {@link LogfileLoader} in order to process incoming logfiles.
 * This Thread is created as a side effect of the first successful POST to the /internal/logs/ servlet.</li>
 * <li>One background Thread runs the {@link SynchronizerTask} which is used to periodically
 * synchronize the database between active and standby servers.</li>
 * <li>One background Thread runs the {@link Poker} which is used to notify the nodes whenever
 * provisioning data changes.</li>
 * <li>One task is run once a day to run {@link PurgeLogDirTask} which purges older logs from the
 * /opt/app/datartr/logs directory.</li>
 * </ul>
 * <p>
 * The provisioning server is stopped by issuing a GET to the URL http://127.0.0.1/internal/halt using <i>curl</i> or
 * some other such tool.
 * </p>
 *
 * @author Robert Eby
 * @version $Id: Main.java,v 1.12 2014/03/12 19:45:41 eby Exp $
 */
public class ProvRunner {

    public static final EELFLogger intlogger = EELFManager.getInstance()
                                                       .getLogger("org.onap.dmaap.datarouter.provisioning.internal");

    private static Server provServer;
    private static AafPropsUtils aafPropsUtils;
    private static Properties provProperties;

    /**
     * Starts the Data Router Provisioning server.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        // Check DB is accessible and contains the expected tables
        if (!ProvDbUtils.getInstance().initProvDB()) {
            intlogger.error("Data Router Provisioning database init failure. Exiting.");
            exit(1);
        }
        // Set up AAF properties
        try {
            aafPropsUtils = new AafPropsUtils(new File(getProvProperties().getProperty(
                "org.onap.dmaap.datarouter.provserver.aafprops.path",
                "/opt/app/osaaf/local/org.onap.dmaap-dr.props")));
        } catch (IOException e) {
            intlogger.error("NODE0314 Failed to load AAF props. Exiting", e);
            exit(1);
        }
        // Daemon to clean up the log directory on a daily basis
        Timer rolex = new Timer();
        rolex.scheduleAtFixedRate(new PurgeLogDirTask(), 0, 86400000L);    // run once per day

        try {
            // Create and start the Jetty server
            provServer = ProvServer.getServerInstance();
            intlogger.info("PROV0000 **** DMaaP Data Router Provisioning Server starting....");
            provServer.start();
            provServer.dumpStdErr();
            provServer.join();
            intlogger.info("PROV0000 **** DMaaP Data Router Provisioning Server started: " + provServer.getState());
        } catch (Exception e) {
            intlogger.error(
                "PROV0010 **** DMaaP Data Router Provisioning Server failed to start. Exiting: " + e.getMessage(), e);
            exit(1);
        }
        // Start LogfileLoader
        LogfileLoader.getLoader();
    }

    /**
     * Stop the Jetty server.
     */
    static void shutdown() {
        new Thread(() -> {
            try {
                provServer.stop();
                Thread.sleep(5000L);
                exit(0);
            } catch (Exception e) {
                intlogger.error("Exception in Main.shutdown(): " + e.getMessage(), e);
            }
        });
    }

    public static Properties getProvProperties() {
        if (provProperties == null) {
            try {
                provProperties = new Properties();
                provProperties.load(new FileInputStream(getProperty(
                    "org.onap.dmaap.datarouter.provserver.properties",
                    "/opt/app/datartr/etc/provserver.properties")));
            } catch (IOException e) {
                intlogger.error("Failed to load PROV properties: " + e.getMessage(), e);
                exit(1);
            }
        }
        return provProperties;
    }

    public static AafPropsUtils getAafPropsUtils() {
        return aafPropsUtils;
    }
}
