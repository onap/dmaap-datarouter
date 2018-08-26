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

import org.apache.log4j.Logger;

import java.util.*;
import java.net.*;

/**
 * Determine if an IP address is from a machine
 */
public class IsFrom {
    private long nextcheck;
    private String[] ips;
    private String fqdn;
    private static Logger logger = Logger.getLogger("org.onap.dmaap.datarouter.node.IsFrom");

    /**
     * Configure the JVM DNS cache to have a 10 second TTL.  This needs to be called very very early or it won't have any effect.
     */
    public static void setDNSCache() {
        java.security.Security.setProperty("networkaddress.cache.ttl", "10");
    }

    /**
     * Create an IsFrom for the specified fully qualified domain name.
     */
    public IsFrom(String fqdn) {
        this.fqdn = fqdn;
    }

    /**
     * Check if an IP address matches.  If it has been more than
     * 10 seconds since DNS was last checked for changes to the
     * IP address(es) of this FQDN, check again.  Then check
     * if the specified IP address belongs to the FQDN.
     */
    public synchronized boolean isFrom(String ip) {
        long now = System.currentTimeMillis();
        if (now > nextcheck) {
            nextcheck = now + 10000;
            Vector<String> v = new Vector<>();
            try {
                InetAddress[] addrs = InetAddress.getAllByName(fqdn);
                for (InetAddress a : addrs) {
                    v.add(a.getHostAddress());
                }
            } catch (UnknownHostException e) {
                logger.debug("IsFrom: UnknownHostEx: " + e.toString(), e);
            }
            ips = v.toArray(new String[v.size()]);
            logger.info("IsFrom: DNS ENTRIES FOR FQDN " + fqdn + " : " + Arrays.toString(ips));
        }
        for (String s : ips) {
            if (s.equals(ip) || s.equals(System.getenv("DMAAP_DR_PROV_SERVICE_HOST"))) {
                return (true);
            }
        }
        return (false);
    }

    /**
     * Return the fully qualified domain name
     */
    public String toString() {
        return (fqdn);
    }
}
