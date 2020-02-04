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

import static org.onap.dmaap.datarouter.provisioning.utils.HttpServletUtils.sendResponseError;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.IOException;
import java.io.InvalidObjectException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

import org.onap.dmaap.datarouter.authz.AuthorizationResponse;
import org.onap.dmaap.datarouter.provisioning.beans.EventLogRecord;
import org.onap.dmaap.datarouter.provisioning.beans.Feed;
import org.onap.dmaap.datarouter.provisioning.eelf.EelfMsgs;



/**
 * This servlet handles provisioning for the &lt;feedURL&gt; which is generated by the provisioning
 * server to handle a particular feed. It supports DELETE to mark the feed as deleted,
 * and GET to retrieve information about the feed, and PUT to modify the feed.
 *
 * @author Robert Eby
 * @version $Id$
 */
@SuppressWarnings("serial")

public class FeedServlet extends ProxyServlet {

    //Adding EELF Logger Rally:US664892
    private static EELFLogger eelfLogger = EELFManager.getInstance().getLogger(FeedServlet.class);

    /**
     * Delete the Feed at the address /feed/&lt;feednumber&gt;.
     * See the <i>Deleting a Feed</i> section in the <b>Provisioning API</b>
     * document for details on how this method should be invoked.
     */
    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        setIpFqdnRequestIDandInvocationIDForEelf("doDelete", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID,
                    req.getHeader(BEHALF_HEADER),getIdFromPath(req) + "");
            EventLogRecord elr = new EventLogRecord(req);
            String message = isAuthorizedForProvisioning(req);
            if (message != null) {
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_FORBIDDEN);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, message, eventlogger);
                return;
            }
            if (isProxyServer()) {
                super.doDelete(req, resp);
                return;
            }
            String bhdr = req.getHeader(BEHALF_HEADER);
            if (bhdr == null) {
                message = MISSING_ON_BEHALF;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            int feedid = getIdFromPath(req);
            if (feedid < 0) {
                message = MISSING_FEED;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            Feed feed = Feed.getFeedById(feedid);
            if (feed == null || feed.isDeleted()) {
                message = MISSING_FEED;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_NOT_FOUND);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, message, eventlogger);
                return;
            }
            /*
             * START - AAF changes
             * TDP EPIC US# 307413
             * CADI code - check on permissions based on Legacy/AAF users to allow to delete/remove feed
             */
            String aafInstance = feed.getAafInstance();
            if (aafInstance == null || "".equals(aafInstance) || "legacy".equalsIgnoreCase(aafInstance)) {
                AuthorizationResponse aresp = authz.decide(req);
                if (! aresp.isAuthorized()) {
                    message = POLICY_ENGINE;
                    elr.setMessage(message);
                    elr.setResult(HttpServletResponse.SC_FORBIDDEN);
                    eventlogger.error(elr.toString());
                    sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, message, eventlogger);
                    return;
                }
            } else {
                String permission = getFeedPermission(aafInstance, BaseServlet.DELETE_PERMISSION);
                eventlogger.info("FeedServlet.doDelete().. Permission String - " + permission);
                if (!req.isUserInRole(permission)) {
                    message = "AAF disallows access to permission - " + permission;
                    elr.setMessage(message);
                    elr.setResult(HttpServletResponse.SC_FORBIDDEN);
                    eventlogger.error(elr.toString());
                    sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, message, eventlogger);
                    return;
                }
            }
            /*
             * END - AAF changes
             */
            // Delete FEED table entry (set DELETED flag)
            feed.setDeleted(true);
            if (doUpdate(feed)) {
                activeFeeds--;
                // send response
                elr.setResult(HttpServletResponse.SC_NO_CONTENT);
                eventlogger.info(elr.toString());
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                provisioningDataChanged();
            } else {
                // Something went wrong with the UPDATE
                elr.setResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, DB_PROBLEM_MSG, eventlogger);
            }
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * Get information on the feed at the address /feed/&lt;feednumber&gt;.
     * See the <i>Retrieving Information about a Feed</i> section in the <b>Provisioning API</b>
     * document for details on how this method should be invoked.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        setIpFqdnRequestIDandInvocationIDForEelf("doGet", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID,
                    req.getHeader(BEHALF_HEADER),getIdFromPath(req) + "");
            EventLogRecord elr = new EventLogRecord(req);
            String message = isAuthorizedForProvisioning(req);
            if (message != null) {
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_FORBIDDEN);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, message, eventlogger);
                return;
            }
            if (isProxyServer()) {
                super.doGet(req, resp);
                return;
            }
            String bhdr = req.getHeader(BEHALF_HEADER);
            if (bhdr == null) {
                message = MISSING_ON_BEHALF;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            int feedid = getIdFromPath(req);
            if (feedid < 0) {
                message = MISSING_FEED;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            Feed feed = Feed.getFeedById(feedid);
            if (feed == null || feed.isDeleted()) {
                message = MISSING_FEED;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_NOT_FOUND);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, message, eventlogger);
                return;
            }
            // Check with the Authorizer
            AuthorizationResponse aresp = authz.decide(req);
            if (! aresp.isAuthorized()) {
                message = POLICY_ENGINE;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_FORBIDDEN);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, message, eventlogger);
                return;
            }

            // send response
            elr.setResult(HttpServletResponse.SC_OK);
            eventlogger.info(elr.toString());
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType(FEEDFULL_CONTENT_TYPE);
            try {
                resp.getOutputStream().print(feed.asJSONObject(true).toString());
            } catch (IOException ioe) {
                eventlogger.error("PROV0101 FeedServlet.doGet: " + ioe.getMessage(), ioe);
            }
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * PUT on the &lt;feedURL&gt; for a feed.
     * See the <i>Modifying a Feed</i> section in the <b>Provisioning API</b>
     * document for details on how this method should be invoked.
     */
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) {
        setIpFqdnRequestIDandInvocationIDForEelf("doPut", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_FEEDID,
                    req.getHeader(BEHALF_HEADER),getIdFromPath(req) + "");
            EventLogRecord elr = new EventLogRecord(req);
            String message = isAuthorizedForProvisioning(req);
            if (message != null) {
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_FORBIDDEN);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, message, eventlogger);
                return;
            }
            if (isProxyServer()) {
                super.doPut(req, resp);
                return;
            }
            String bhdr = req.getHeader(BEHALF_HEADER);
            if (bhdr == null) {
                message = MISSING_ON_BEHALF;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            int feedid = getIdFromPath(req);
            if (feedid < 0) {
                message = MISSING_FEED;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            Feed oldFeed = Feed.getFeedById(feedid);
            if (oldFeed == null || oldFeed.isDeleted()) {
                message = MISSING_FEED;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_NOT_FOUND);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, message, eventlogger);
                return;
            }
            // check content type is FEED_CONTENT_TYPE, version 1.0
            ContentHeader ch = getContentHeader(req);
            String ver = ch.getAttribute("version");
            if (!ch.getType().equals(FEED_BASECONTENT_TYPE) || !("1.0".equals(ver) || "2.0".equals(ver))) {
                message = "Incorrect content-type";
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, message, eventlogger);
                return;
            }
            JSONObject jo = getJSONfromInput(req);
            if (jo == null) {
                message = BAD_JSON;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            Feed feed;
            try {
                feed = new Feed(jo);
            } catch (InvalidObjectException e) {
                message = e.getMessage();
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString(), e);
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            feed.setFeedid(feedid);
            feed.setPublisher(bhdr);    // set from X-DMAAP-DR-ON-BEHALF-OF header

            //Adding for group feature:Rally US708115
            String subjectgroup = (req.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP"));
            if (!oldFeed.getPublisher().equals(feed.getPublisher()) && subjectgroup == null) {
                message = "This feed must be modified by the same publisher that created it.";
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            if (!oldFeed.getName().equals(feed.getName())) {
                message = "The name of the feed may not be updated.";
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            //  US DSCDR-19 for DCAE if version is not null, version can't be changed
            if ((oldFeed.getVersion() != null) && (feed.getVersion() != null)
                        && !oldFeed.getVersion().equals(feed.getVersion())) {
                message = "The version of the feed may not be updated.";
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }

            /*
             * START - AAF changes
             * TDP EPIC US# 307413
             * CADI code - check on permissions based on Legacy/AAF users to allow feed edit/update/modify
             */
            String aafInstance = feed.getAafInstance();
            if (aafInstance == null || "".equals(aafInstance) || "legacy".equalsIgnoreCase(aafInstance)) {
                // Check with the Authorizer
                AuthorizationResponse aresp = authz.decide(req);
                if (!aresp.isAuthorized()) {
                    message = POLICY_ENGINE;
                    elr.setMessage(message);
                    elr.setResult(HttpServletResponse.SC_FORBIDDEN);
                    eventlogger.error(elr.toString());
                    sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, message, eventlogger);
                    return;
                }
            } else {
                String permission = getFeedPermission(aafInstance, BaseServlet.EDIT_PERMISSION);
                eventlogger.info("FeedServlet.doPut().. Permission String - " + permission);
                if (!req.isUserInRole(permission)) {
                    message = "AAF disallows access to permission - " + permission;
                    elr.setMessage(message);
                    elr.setResult(HttpServletResponse.SC_FORBIDDEN);
                    eventlogger.error(elr.toString());
                    sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, message, eventlogger);
                    return;
                }
            }
            /*
             * END - AAF changes
             */

            // Update FEEDS table entries
            if (doUpdate(feed)) {
                // send response
                elr.setResult(HttpServletResponse.SC_OK);
                eventlogger.info(elr.toString());
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType(FEEDFULL_CONTENT_TYPE);
                try {
                    resp.getOutputStream().print(feed.asLimitedJSONObject().toString());
                } catch (IOException ioe) {
                    eventlogger.error("PROV0102 FeedServlet.doPut: " + ioe.getMessage(), ioe);
                }


                /**Change Owner ship of Feed //Adding for group feature. :Rally US708115*/
                if (jo.has("changeowner") && subjectgroup != null) {
                    try {
                        Boolean changeowner = (Boolean) jo.get("changeowner");
                        if (changeowner != null && changeowner.equals(true)) {
                            feed.setPublisher(req.getHeader(BEHALF_HEADER));
                            feed.changeOwnerShip();
                        }
                    } catch (JSONException je) {
                        eventlogger.error("PROV0103 FeedServlet.doPut: " + je.getMessage(), je);
                    }
                }
                /***End of change ownership.*/

                provisioningDataChanged();
            } else {
                // Something went wrong with the UPDATE
                elr.setResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, DB_PROBLEM_MSG, eventlogger);
            }
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * POST on the &lt;feedURL&gt; -- not supported.
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        setIpFqdnRequestIDandInvocationIDForEelf("doPost", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF, req.getHeader(BEHALF_HEADER));
            String message = "POST not allowed for the feedURL.";
            EventLogRecord elr = new EventLogRecord(req);
            elr.setMessage(message);
            elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            eventlogger.error(elr.toString());
            sendResponseError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, eventlogger);
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }
}
