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

/**
 * Information for a delivery destination that doesn't change from message to message.
 */
public class DestInfo {

    private String name;
    private String spool;
    private String subid;
    private String logdata;
    private String url;
    private String authuser;
    private String authentication;
    private boolean metaonly;
    private boolean use100;
    private boolean privilegedSubscriber;
    private boolean decompress;
    private boolean followRedirects;

    /**
     * Create a destination information object.
     *
     * @param destInfoBuilder DestInfo Object Builder
     */
    public DestInfo(DestInfoBuilder destInfoBuilder) {
        this.name = destInfoBuilder.getName();
        this.spool = destInfoBuilder.getSpool();
        this.subid = destInfoBuilder.getSubid();
        this.logdata = destInfoBuilder.getLogdata();
        this.url = destInfoBuilder.getUrl();
        this.authuser = destInfoBuilder.getAuthuser();
        this.authentication = destInfoBuilder.getAuthentication();
        this.metaonly = destInfoBuilder.isMetaonly();
        this.use100 = destInfoBuilder.isUse100();
        this.privilegedSubscriber = destInfoBuilder.isPrivilegedSubscriber();
        this.followRedirects = destInfoBuilder.isFollowRedirects();
        this.decompress = destInfoBuilder.isDecompress();
    }

    /**
     * Create a destination information object.
     *
     * @param name n:fqdn or s:subid
     * @param spool The directory where files are spooled.
     * @param subscription The subscription.
     */
    public DestInfo(String name, String spool, NodeConfig.ProvSubscription subscription) {
        this.name = name;
        this.spool = spool;
        this.subid = subscription.getSubId();
        this.logdata = subscription.getFeedId();
        this.url = subscription.getURL();
        this.authuser = subscription.getAuthUser();
        this.authentication = subscription.getCredentials();
        this.metaonly = subscription.isMetaDataOnly();
        this.use100 = subscription.isUsing100();
        this.privilegedSubscriber = subscription.isPrivilegedSubscriber();
        this.followRedirects = subscription.getFollowRedirect();
        this.decompress = subscription.isDecompress();
    }

    public boolean equals(Object object) {
        return ((object instanceof DestInfo) && ((DestInfo) object).spool.equals(spool));
    }

    public int hashCode() {
        return (spool.hashCode());
    }

    /**
     * Get the name of this destination.
     */
    public String getName() {
        return (name);
    }

    /**
     * Get the spool directory for this destination.
     *
     * @return The spool directory
     */
    public String getSpool() {
        return (spool);
    }

    /**
     * Get the subscription ID.
     *
     * @return Subscription ID or null if this is a node to node delivery.
     */
    public String getSubId() {
        return (subid);
    }

    /**
     * Get the log data.
     *
     * @return Text to be included in a log message about delivery attempts.
     */
    public String getLogData() {
        return (logdata);
    }

    /**
     * Get the delivery URL.
     *
     * @return The URL to deliver to (the primary URL).
     */
    public String getURL() {
        return (url);

    }

    /**
     * Get the user for authentication.
     *
     * @return The name of the user for logging
     */
    public String getAuthUser() {
        return (authuser);
    }

    /**
     * Get the authentication header.
     *
     * @return The string to use to authenticate to the recipient.
     */
    public String getAuth() {
        return (authentication);
    }

    /**
     * Is this a metadata only delivery.
     *
     * @return True if this is a metadata only delivery
     */
    public boolean isMetaDataOnly() {
        return (metaonly);
    }

    /**
     * Should I send expect 100-continue header.
     *
     * @return True if I should.
     */
    public boolean isUsing100() {
        return (use100);
    }

    /**
     * Should we wait to receive a file processed acknowledgement before deleting file.
     */
    public boolean isPrivilegedSubscriber() {
        return (privilegedSubscriber);
    }

    /**
     * Should I follow redirects.
     *
     * @return True if I should.
     */
    public boolean isFollowRedirects() {
        return (followRedirects);
    }

    /**
     * Should i decompress the file before sending it on.
     *
     * @return True if I should.
     */
    public boolean isDecompress() {
        return (decompress);
    }


}
