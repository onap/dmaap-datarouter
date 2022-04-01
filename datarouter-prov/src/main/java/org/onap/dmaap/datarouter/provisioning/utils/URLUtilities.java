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
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.onap.dmaap.datarouter.provisioning.BaseServlet;
import org.onap.dmaap.datarouter.provisioning.ProvRunner;

/**
 * Utility functions used to generate the different URLs used by the Data Router.
 *
 * @author Robert Eby
 * @version $Id: URLUtilities.java,v 1.2 2014/03/12 19:45:41 eby Exp $
 */
public class URLUtilities {

    private static final EELFLogger utilsLogger = EELFManager.getInstance().getLogger("UtilsLog");
    private static String otherPod;

    private URLUtilities() {
    }

    /**
     * Generate the URL used to access a feed.
     *
     * @param feedid the feed id
     * @return the URL
     */
    public static String generateFeedURL(int feedid) {
        return getUrlSecurityOption() + BaseServlet.getProvName() + "/feed/" + feedid;
    }

    /**
     * Generate the URL used to publish to a feed.
     *
     * @param feedid the feed id
     * @return the URL
     */
    public static String generatePublishURL(int feedid) {
        return getUrlSecurityOption() + BaseServlet.getProvName() + "/publish/" + feedid;
    }

    /**
     * Generate the URL used to subscribe to a feed.
     *
     * @param feedid the feed id
     * @return the URL
     */
    public static String generateSubscribeURL(int feedid) {
        return getUrlSecurityOption() + BaseServlet.getProvName() + "/subscribe/" + feedid;
    }

    /**
     * Generate the URL used to access a feed's logs.
     *
     * @param feedid the feed id
     * @return the URL
     */
    public static String generateFeedLogURL(int feedid) {
        return getUrlSecurityOption() + BaseServlet.getProvName() + "/feedlog/" + feedid;
    }

    /**
     * Generate the URL used to access a subscription.
     *
     * @param subid the subscription id
     * @return the URL
     */
    public static String generateSubscriptionURL(int subid) {
        return getUrlSecurityOption() + BaseServlet.getProvName() + "/subs/" + subid;
    }

    /**
     * Generate the URL used to access a subscription's logs.
     *
     * @param subid the subscription id
     * @return the URL
     */
    public static String generateSubLogURL(int subid) {
        return getUrlSecurityOption() + BaseServlet.getProvName() + "/sublog/" + subid;
    }

    /**
     * Generate the URL used to access the provisioning data on the peer POD.
     *
     * @return the URL
     */
    public static String generatePeerProvURL() {
        return getUrlSecurityOption() + getPeerPodName() + "/internal/prov";
    }

    /**
     * Generate the URL used to access the logfile data on the peer POD.
     *
     * @return the URL
     */
    public static String generatePeerLogsURL() {
        //Fixes for Itrack ticket - DATARTR-4#Fixing if only one Prov is configured, not to give exception to fill logs.
        String peerPodUrl = getPeerPodName();
        if (peerPodUrl == null || "".equals(peerPodUrl)) {
            return "";
        }

        return getUrlSecurityOption() + peerPodUrl + "/internal/drlogs/";
    }

    /**
     * Return the real (non CNAME) version of the peer POD's DNS name.
     *
     * @return the name
     */
    public static String getPeerPodName() {
        if (otherPod == null) {
            String thisPod;
            try {
                thisPod = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                utilsLogger.trace("UnkownHostException: " + e.getMessage(), e);
                thisPod = "";
            }
            for (String pod : BaseServlet.getPods()) {
                if (!pod.equals(thisPod)) {
                    otherPod = pod;
                }
            }
        }
        return otherPod;
    }

    public static String getUrlSecurityOption() {
        if (Boolean.parseBoolean(ProvRunner.getProvProperties()
            .getProperty("org.onap.dmaap.datarouter.provserver.tlsenabled", "true"))) {
            return "https://";
        }
        return "http://";
    }

}
