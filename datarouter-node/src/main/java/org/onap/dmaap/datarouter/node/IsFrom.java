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

import java.io.IOException;
import java.util.*;
import java.net.*;

/**
 * Determine if an IP address is from a machine
 */
public class IsFrom {
    private long nextcheck;
    private String[] ips;
    private String fqdn;
    private static EELFLogger logger = EELFManager.getInstance().getLogger(IsFrom.class);

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
            ArrayList<String> hostAddrArray = new ArrayList<>();
            try {
                InetAddress[] addrs = InetAddress.getAllByName(fqdn);
                for (InetAddress addr : addrs) {
                    hostAddrArray.add(addr.getHostAddress());
                }
            } catch (UnknownHostException e) {
                logger.error("IsFrom: UnknownHostEx: " + e.toString(), e);
            }
            ips = hostAddrArray.toArray(new String[0]);
            logger.info("IsFrom: DNS ENTRIES FOR FQDN " + fqdn + " : " + Arrays.toString(ips));
        }
        for (String ipAddr : ips) {
            if (ipAddr.equals(ip)) {
                return true;
            }
        }
        return false;
    }

    synchronized boolean isReachable(String ip) {
        try {
            if (InetAddress.getByName(ip).isReachable(1000)) {
                return true;
            }
        } catch (UnknownHostException e) {
            logger.error("IsFrom: UnknownHostEx: " + e.toString(), e);
        } catch (IOException e) {
            logger.error("IsFrom: Failed to parse IP : " + ip + " : " + e.toString(), e);
        }
        return false;
    }

    /**
     * Return the fully qualified domain name
     */
    public String toString() {
        return (fqdn);
    }
}
