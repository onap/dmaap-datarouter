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
 * Interface to allow independent testing of the DeliveryQueue code
 * <p>
 * This interface represents all of the configuration information and
 * feedback mechanisms that a delivery queue needs.
 */
public interface DeliveryQueueHelper {
    /**
     * Get the timeout (milliseconds) before retrying after an initial delivery failure
     */
    public long getInitFailureTimer();

    /**
     * Get the ratio between timeouts on consecutive delivery attempts
     */
    public double getFailureBackoff();

    /**
     * Get the maximum timeout (milliseconds) between delivery attempts
     */
    public long getMaxFailureTimer();

    /**
     * Get the expiration timer (milliseconds) for deliveries
     */
    public long getExpirationTimer();

    /**
     * Get the maximum number of file delivery attempts before checking
     * if another queue has work to be performed.
     */
    public int getFairFileLimit();

    /**
     * Get the maximum amount of time spent delivering files before checking if another queue has work to be performed.
     */
    public long getFairTimeLimit();

    /**
     * Get the URL for delivering a file
     *
     * @param dest   The destination information for the file to be delivered.
     * @param fileid The file id for the file to be delivered.
     * @return The URL for delivering the file (typically, dest.getURL() + "/" + fileid).
     */
    public String getDestURL(DestInfo dest, String fileid);

    /**
     * Forget redirections associated with a subscriber
     *
     * @param    dest    Destination information to forget
     */
    public void handleUnreachable(DestInfo dest);

    /**
     * Post redirection for a subscriber
     *
     * @param    dest    Destination information to update
     * @param    location    Location given by subscriber
     * @param    fileid    File ID of request
     * @return true if this 3xx response is retryable, otherwise, false.
     */
    public boolean handleRedirection(DestInfo dest, String location, String fileid);

    /**
     * Should I handle 3xx responses differently than 4xx responses?
     */
    public boolean isFollowRedirects();

    /**
     * Get the feed ID for a subscription
     *
     * @param subid The subscription ID
     * @return The feed ID
     */
    public String getFeedId(String subid);
}
