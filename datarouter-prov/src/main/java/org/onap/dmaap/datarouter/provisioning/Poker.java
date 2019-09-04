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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onap.dmaap.datarouter.provisioning.beans.EgressRoute;
import org.onap.dmaap.datarouter.provisioning.beans.Feed;
import org.onap.dmaap.datarouter.provisioning.beans.Group;
import org.onap.dmaap.datarouter.provisioning.beans.IngressRoute;
import org.onap.dmaap.datarouter.provisioning.beans.NetworkRoute;
import org.onap.dmaap.datarouter.provisioning.beans.Parameters;
import org.onap.dmaap.datarouter.provisioning.beans.Subscription;
import org.onap.dmaap.datarouter.provisioning.utils.DbConnectionPool;

/**
 * This class handles the two timers (described in R1 Design Notes), and takes care of issuing the GET to each node of
 * the URL to "poke".
 *
 * @author Robert Eby
 * @version $Id: Poker.java,v 1.11 2014/01/08 16:13:47 eby Exp $
 */

public class Poker extends TimerTask {

    /**
     * Template used to generate the URL to issue the GET against.
     */
    private static final String POKE_URL_TEMPLATE = "http://%s/internal/fetchProv";

    private static final Object lock = new Object();
    private static final String CARRIAGE_RETURN = "\n],\n";

    /**
     * This is a singleton -- there is only one Poker object in the server.
     */
    private static Poker poker;
    private long timer1;
    private long timer2;
    private String thisPod;        // DNS name of this machine
    private EELFLogger logger;
    private String provString;


    private Poker() {
        timer1 = timer2 = 0;
        logger = EELFManager.getInstance().getLogger("InternalLog");
        try {
            thisPod = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            thisPod = "*UNKNOWN_POD*"; // not a major problem
            logger.info("UnknownHostException: Setting thisPod to \"*UNKNOWN_POD*\"", e);
        }
        provString = buildProvisioningString();
        Timer rolex = new Timer();
        rolex.scheduleAtFixedRate(this, 0L, 1000L);    // Run once a second to check the timers
    }

    /**
     * Get the singleton Poker object.
     *
     * @return the Poker
     */
    public static synchronized Poker getPoker() {
        if (poker == null) {
            poker = new Poker();
        }
        return poker;
    }

    /**
     * This method sets the two timers described in the design notes.
     *
     * @param t1 the first timer controls how long to wait after a provisioning request before poking each node This
     * timer can be reset if it has not "gone off".
     * @param t2 the second timer set the outer bound on how long to wait.  It cannot be reset.
     */
    public void setTimers(long t1, long t2) {
        synchronized (lock) {
            if (timer1 == 0 || t1 > timer1) {
                timer1 = t1;
            }
            if (timer2 == 0) {
                timer2 = t2;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Poker timers set to " + timer1 + " and " + timer2);
        }


    }

    /**
     * Return the last provisioning string built.
     *
     * @return the last provisioning string built.
     */
    public String getProvisioningString() {
        return provString;
    }

    /**
     * The method to run at the predefined interval (once per second).  This method checks to see if either of the two
     * timers has expired, and if so, will rebuild the provisioning string, and poke all the nodes and other PODs.  The
     * timers are then reset to 0.
     */
    @Override
    public void run() {
        try {
            if (timer1 > 0) {
                long now = System.currentTimeMillis();
                boolean fire = false;
                synchronized (lock) {
                    if (now > timer1 || now > timer2) {
                        timer1 = timer2 = 0;
                        fire = true;
                    }
                }
                if (fire) {
                    pokeNodes();
                }
            }
        } catch (Exception e) {
            logger.warn("PROV0020: Caught exception in Poker: " + e);
        }
    }

    private void pokeNodes() {
        // Rebuild the prov string
        provString = buildProvisioningString();
        // Only the active POD should poke nodes, etc.
        boolean active = SynchronizerTask.getSynchronizer().isActive();
        if (active) {
            // Poke all the DR nodes
            for (String n : BaseServlet.getNodes()) {
                pokeNode(n);
            }
            // Poke the pod that is not us
            for (String n : BaseServlet.getPods()) {
                if (n.length() > 0 && !n.equals(thisPod)) {
                    pokeNode(n);
                }
            }
        }
    }

    private void pokeNode(final String nodename) {
        logger.debug("PROV0012 Poking node " + nodename + " ...");
        String nodeUrl = String.format(POKE_URL_TEMPLATE, nodename + ":" + DbConnectionPool.getHttpPort());
        Runnable runn = () -> {
            try {
                URL url = new URL(nodeUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(60000);    //Fixes for Itrack DATARTR-3, poke timeout
                conn.connect();
                conn.getContentLength();    // Force the GET through
                conn.disconnect();
            } catch (MalformedURLException e) {
                logger.warn(
                        "PROV0013 MalformedURLException Error poking node at " + nodeUrl + " : " + e
                                .getMessage(), e);
            } catch (IOException e) {
                logger.warn("PROV0013 IOException Error poking node at " + nodeUrl + " : " + e
                        .getMessage(), e);
            }
        };
        runn.run();
    }

    private String buildProvisioningString() {
        StringBuilder sb = new StringBuilder("{\n");

        // Append Feeds to the string
        String pfx = "\n";
        sb.append("\"feeds\": [");
        for (Feed f : Feed.getAllFeeds()) {
            sb.append(pfx);
            sb.append(f.asJSONObject().toString());
            pfx = ",\n";
        }
        sb.append(CARRIAGE_RETURN);

        //Append groups to the string - Rally:US708115  - 1610
        pfx = "\n";
        sb.append("\"groups\": [");
        for (Group s : Group.getAllgroups()) {
            sb.append(pfx);
            sb.append(s.asJSONObject().toString());
            pfx = ",\n";
        }
        sb.append(CARRIAGE_RETURN);

        // Append Subscriptions to the string
        pfx = "\n";
        sb.append("\"subscriptions\": [");
        for (Subscription s : Subscription.getAllSubscriptions()) {
            sb.append(pfx);
            if (s != null) {
                sb.append(s.asJSONObject().toString());
            }
            pfx = ",\n";
        }
        sb.append(CARRIAGE_RETURN);

        // Append Parameters to the string
        pfx = "\n";
        sb.append("\"parameters\": {");
        Map<String, String> props = Parameters.getParameters();
        Set<String> ivals = new HashSet<>();
        String intv = props.get("_INT_VALUES");
        if (intv != null) {
            ivals.addAll(Arrays.asList(intv.split("\\|")));
        }
        for (String key : new TreeSet<String>(props.keySet())) {
            String val = props.get(key);
            sb.append(pfx);
            sb.append("  \"").append(key).append("\": ");
            if (ivals.contains(key)) {
                // integer value
                sb.append(val);
            } else if (key.endsWith("S")) {
                // Split and append array of strings
                String[] pp = val.split("\\|");
                String p2 = "";
                sb.append("[");
                for (String t : pp) {
                    sb.append(p2).append("\"").append(quote(t)).append("\"");
                    p2 = ",";
                }
                sb.append("]");
            } else {
                sb.append("\"").append(quote(val)).append("\"");
            }
            pfx = ",\n";
        }
        sb.append("\n},\n");

        // Append Routes to the string
        pfx = "\n";
        sb.append("\"ingress\": [");
        for (IngressRoute in : IngressRoute.getAllIngressRoutes()) {
            sb.append(pfx);
            sb.append(in.asJSONObject().toString());
            pfx = ",\n";
        }
        sb.append(CARRIAGE_RETURN);

        pfx = "\n";
        sb.append("\"egress\": {");
        for (EgressRoute eg : EgressRoute.getAllEgressRoutes()) {
            sb.append(pfx);
            String str = eg.asJSONObject().toString();
            str = str.substring(1, str.length() - 1);
            sb.append(str);
            pfx = ",\n";
        }
        sb.append("\n},\n");

        pfx = "\n";
        sb.append("\"routing\": [");
        for (NetworkRoute ne : NetworkRoute.getAllNetworkRoutes()) {
            sb.append(pfx);
            sb.append(ne.asJSONObject().toString());
            pfx = ",\n";
        }
        sb.append("\n]");
        sb.append("\n}");

        // Convert to string and verify it is valid JSON
        String tempProvString = sb.toString();
        try {
            new JSONObject(new JSONTokener(tempProvString));
        } catch (JSONException e) {
            logger.warn("PROV0016: Possible invalid prov string: " + e);
        }
        return tempProvString;
    }

    private String quote(String str) {
        StringBuilder sb = new StringBuilder();
        for (char ch : str.toCharArray()) {
            if (ch == '\\' || ch == '"') {
                sb.append('\\');
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}
