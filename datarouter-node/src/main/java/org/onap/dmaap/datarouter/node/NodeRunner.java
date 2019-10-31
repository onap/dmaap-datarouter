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

package org.onap.dmaap.datarouter.node;

import static java.lang.System.exit;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.eclipse.jetty.server.Server;

/**
 * The main starting point for the Data Router node.
 */
public class NodeRunner {

    private static EELFLogger nodeMainLogger = EELFManager.getInstance().getLogger(NodeRunner.class);
    private static NodeConfigManager nodeConfigManager;

    private NodeRunner() {
    }

    /**
     * Start the data router.
     *
     * <p>The location of the node configuration file can be set using the org.onap.dmaap.datarouter.node.properties
     * system property. By default, it is "/opt/app/datartr/etc/node.properties".
     */
    public static void main(String[] args) {
        nodeMainLogger.debug("NODE0001 Data Router Node Starting");
        IsFrom.setDNSCache();
        nodeConfigManager = NodeConfigManager.getInstance();
        nodeMainLogger.debug("NODE0002 I am " + nodeConfigManager.getMyName());
        (new WaitForConfig(nodeConfigManager)).waitForConfig();
        new LogManager(nodeConfigManager);
        try {
            Server server = NodeServer.getServerInstance();
            server.start();
            server.join();
            nodeMainLogger.debug("NODE00006 Node Server started-" + server.getState());
        } catch (Exception e) {
            nodeMainLogger.error("NODE00006 Jetty failed to start. Reporting will we be unavailable: "
                                         + e.getMessage(), e);
            exit(1);
        }
        nodeMainLogger.debug("NODE00007 Node Server joined");
    }

    private static class WaitForConfig implements Runnable {

        private NodeConfigManager localNodeConfigManager;

        WaitForConfig(NodeConfigManager ncm) {
            this.localNodeConfigManager = ncm;
        }

        public synchronized void run() {
            notifyAll();
        }

        synchronized void waitForConfig() {
            localNodeConfigManager.registerConfigTask(this);
            while (!localNodeConfigManager.isConfigured()) {
                nodeMainLogger.debug("NODE0003 Waiting for Node Configuration");
                try {
                    wait();
                } catch (Exception exception) {
                    nodeMainLogger.error("NodeMain: waitForConfig exception. Exception Message:- "
                        + exception.toString(), exception);
                }
            }
            localNodeConfigManager.deregisterConfigTask(this);
            nodeMainLogger.debug("NODE0004 Node Configuration Data Received");
        }
    }
}
