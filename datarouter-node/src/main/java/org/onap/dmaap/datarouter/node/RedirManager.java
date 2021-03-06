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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

/**
 * Track redirections of subscriptions.
 */
class RedirManager {

    private static EELFLogger eelfLogger = EELFManager.getInstance().getLogger(RedirManager.class);
    private RateLimitedOperation op;
    private HashMap<String, String> sid2primary = new HashMap<>();
    private HashMap<String, String> sid2secondary = new HashMap<>();
    private String redirfile;

    /**
     * Create a mechanism for maintaining subscription redirections.
     *
     * @param redirfile The file to store the redirection information.
     * @param mininterval The minimum number of milliseconds between writes to the redirection information file.
     * @param timer The timer thread used to run delayed file writes.
     */
    RedirManager(String redirfile, long mininterval, Timer timer) {
        this.redirfile = redirfile;
        op = new RateLimitedOperation(mininterval, timer) {
            public void run() {
                try {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> entry : sid2primary.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        sb.append(key).append(' ').append(value).append(' ')
                                .append(sid2secondary.get(key)).append('\n');
                    }
                    try (OutputStream os = new FileOutputStream(RedirManager.this.redirfile)) {
                        os.write(sb.toString().getBytes());
                    }
                } catch (Exception e) {
                    eelfLogger.error("Exception", e);
                }
            }
        };
        try {
            String line;
            try (BufferedReader br = new BufferedReader(new FileReader(redirfile))) {
                while ((line = br.readLine()) != null) {
                    addSubRedirInfo(line);
                }
            }
        } catch (Exception e) {
            eelfLogger.debug("Missing file is normal", e);
        }
    }

    /**
     * Set up redirection.  If a request is to be sent to subscription ID sid, and that is configured to go to URL
     * primary, instead, go to secondary.
     *
     * @param sid The subscription ID to be redirected
     * @param primary The URL associated with that subscription ID
     * @param secondary The replacement URL to use instead
     */
    synchronized void redirect(String sid, String primary, String secondary) {
        sid2primary.put(sid, primary);
        sid2secondary.put(sid, secondary);
        op.request();
    }

    /**
     * Cancel redirection.  If a request is to be sent to subscription ID sid, send it to its primary URL.
     *
     * @param sid The subscription ID to remove from the table.
     */
    synchronized void forget(String sid) {
        sid2primary.remove(sid);
        sid2secondary.remove(sid);
        op.request();
    }

    /**
     * Look up where to send a subscription.  If the primary has changed or there is no redirection, use the primary.
     * Otherwise, redirect to the secondary URL.
     *
     * @param sid The subscription ID to look up.
     * @param primary The configured primary URL.
     * @return The destination URL to really use.
     */
    synchronized String lookup(String sid, String primary) {
        String oprim = sid2primary.get(sid);
        if (primary.equals(oprim)) {
            return (sid2secondary.get(sid));
        } else if (oprim != null) {
            forget(sid);
        }
        return (primary);
    }

    /**
     * Is a subscription redirected.
     */
    synchronized boolean isRedirected(String sid) {
        return (sid != null && sid2secondary.get(sid) != null);
    }

    private void addSubRedirInfo(String subRedirInfo) {
        subRedirInfo = subRedirInfo.trim();
        String[] sx = subRedirInfo.split(" ");
        if (subRedirInfo.startsWith("#") || sx.length != 3) {
            return;
        }
        sid2primary.put(sx[0], sx[1]);
        sid2secondary.put(sx[0], sx[2]);
    }
}
