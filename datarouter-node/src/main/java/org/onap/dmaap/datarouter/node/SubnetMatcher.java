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

import java.net.*;

/**
 * Compare IP addresses as byte arrays to a subnet specified as a CIDR
 */
public class SubnetMatcher {
    private byte[] sn;
    private int len;
    private int mask;

    /**
     * Construct a subnet matcher given a CIDR
     *
     * @param subnet The CIDR to match
     */
    public SubnetMatcher(String subnet) {
        int i = subnet.lastIndexOf('/');
        if (i == -1) {
            sn = NodeUtils.getInetAddress(subnet);
            len = sn.length;
        } else {
            len = Integer.parseInt(subnet.substring(i + 1));
            sn = NodeUtils.getInetAddress(subnet.substring(0, i));
            mask = ((0xff00) >> (len % 8)) & 0xff;
            len /= 8;
        }
    }

    /**
     * Is the IP address in the CIDR?
     *
     * @param addr the IP address as bytes in network byte order
     * @return true if the IP address matches.
     */
    public boolean matches(byte[] addr) {
        if (addr.length != sn.length) {
            return (false);
        }
        for (int i = 0; i < len; i++) {
            if (addr[i] != sn[i]) {
                return (false);
            }
        }
        if (mask != 0 && ((addr[len] ^ sn[len]) & mask) != 0) {
            return (false);
        }
        return (true);
    }
}
