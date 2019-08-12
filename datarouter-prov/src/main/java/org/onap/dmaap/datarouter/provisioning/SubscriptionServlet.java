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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.authz.AuthorizationResponse;
import org.onap.dmaap.datarouter.provisioning.beans.EventLogRecord;
import org.onap.dmaap.datarouter.provisioning.beans.Subscription;
import org.onap.dmaap.datarouter.provisioning.eelf.EelfMsgs;

/**
 * This servlet handles provisioning for the &lt;subscriptionURL&gt; which is generated by the provisioning server to
 * handle the inspection, modification, and deletion of a particular subscription to a feed. It supports DELETE to
 * delete a subscription, GET to retrieve information about the subscription, and PUT to modify the subscription.  In DR
 * 3.0, POST is also supported in order to reset the subscription timers for individual subscriptions.
 *
 * @author Robert Eby
 * @version $Id$
 */
@SuppressWarnings("serial")
public class SubscriptionServlet extends ProxyServlet {

    private static final String SUBCNTRL_CONTENT_TYPE = "application/vnd.dmaap-dr.subscription-control";
    //Adding EELF Logger Rally:US664892
    private static EELFLogger eelfLogger = EELFManager.getInstance()
        .getLogger(SubscriptionServlet.class);





    /**
     * DELETE on the &lt;subscriptionUrl&gt; -- delete a subscription. See the <i>Deleting a Subscription</i> section in
     * the <b>Provisioning API</b> document for details on how this method should be invoked.
     */
    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        setIpFqdnRequestIDandInvocationIDForEelf("doDelete", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_SUBID,
                    req.getHeader(BEHALF_HEADER), getIdFromPath(req) + "");
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
            int subid = getIdFromPath(req);
            if (subid < 0) {
                message = BAD_SUB;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            Subscription sub = Subscription.getSubscriptionById(subid);
            if (sub == null) {
                message = BAD_SUB;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_NOT_FOUND);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, message, eventlogger);
                return;
            }
            /*
             * START - AAF changes
             * TDP EPIC US# 307413
             * CADI code - check on permissions based on Legacy/AAF users to allow to delete/remove subscription
             */
            String aafInstance = sub.getAafInstance();
            if (aafInstance == null || "".equals(aafInstance) || "legacy".equalsIgnoreCase(aafInstance)) {
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
                String permission = getSubscriberPermission(aafInstance, BaseServlet.DELETE_PERMISSION);
                eventlogger.info("SubscriptionServlet.doDelete().. Permission String - " + permission);
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
            // Delete Subscription
            if (doDelete(sub)) {
                activeSubs--;
                // send response
                elr.setResult(HttpServletResponse.SC_NO_CONTENT);
                eventlogger.info(elr.toString());
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                provisioningDataChanged();
            } else {
                // Something went wrong with the DELETE
                elr.setResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, DB_PROBLEM_MSG, intlogger);
            }
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * GET on the &lt;subscriptionUrl&gt; -- get information about a subscription. See the <i>Retreiving Information
     * about a Subscription</i> section in the <b>Provisioning API</b> document for details on how this method should be
     * invoked.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        setIpFqdnRequestIDandInvocationIDForEelf("doGet", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_SUBID,
                    req.getHeader(BEHALF_HEADER), getIdFromPath(req) + "");
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
            int subid = getIdFromPath(req);
            if (subid < 0) {
                message = BAD_SUB;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            Subscription sub = Subscription.getSubscriptionById(subid);
            if (sub == null) {
                message = BAD_SUB;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_NOT_FOUND);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, message, eventlogger);
                return;
            }
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

            // send response
            elr.setResult(HttpServletResponse.SC_OK);
            eventlogger.info(elr.toString());
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType(SUBFULL_CONTENT_TYPE);
            try {
                resp.getOutputStream().print(sub.asJSONObject(true).toString());
            } catch (IOException ioe) {
                eventlogger.error("PROV0191 SubscriptionServlet.doGet: " + ioe.getMessage(), ioe);
            }
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * PUT on the &lt;subscriptionUrl&gt; -- modify a subscription. See the <i>Modifying a Subscription</i> section in
     * the <b>Provisioning API</b> document for details on how this method should be invoked.
     */
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) {
        setIpFqdnRequestIDandInvocationIDForEelf("doPut", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_SUBID,
                    req.getHeader(BEHALF_HEADER), getIdFromPath(req) + "");
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
            int subid = getIdFromPath(req);
            if (subid < 0) {
                message = BAD_SUB;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            Subscription oldsub = Subscription.getSubscriptionById(subid);
            if (oldsub == null) {
                message = BAD_SUB;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_NOT_FOUND);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_NOT_FOUND, message, eventlogger);
                return;
            }
            // check content type is SUB_CONTENT_TYPE, version 1.0
            ContentHeader ch = getContentHeader(req);
            String ver = ch.getAttribute("version");
            if (!ch.getType().equals(SUB_BASECONTENT_TYPE) || !("1.0".equals(ver) || "2.0".equals(ver))) {
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
            if (intlogger.isDebugEnabled()) {
                intlogger.debug(jo.toString());
            }
            Subscription sub = null;
            try {
                sub = new Subscription(jo);
            } catch (InvalidObjectException e) {
                message = e.getMessage();
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString(), e);
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }

            /*
             * START - AAF changes
             * TDP EPIC US# 307413
             * CADI code - check on permissions based on Legacy/AAF users to allow to delete/remove subscription
             */
            String aafInstance = sub.getAafInstance();
            if (aafInstance == null || "".equals(aafInstance) || "legacy".equalsIgnoreCase(aafInstance)) {
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
                String permission = getSubscriberPermission(aafInstance, BaseServlet.EDIT_PERMISSION);
                eventlogger.info("SubscriptionServlet.doDelete().. Permission String - " + permission);
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
            sub.setSubid(oldsub.getSubid());
            sub.setFeedid(oldsub.getFeedid());
            sub.setSubscriber(bhdr);    // set from X-DMAAP-DR-ON-BEHALF-OF header
            //Adding for group feature:Rally US708115
            String subjectgroup = (req.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP"));
            if (!oldsub.getSubscriber().equals(sub.getSubscriber()) && subjectgroup == null) {
                message = "This subscriber must be modified by the same subscriber that created it.";
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }

            // Update SUBSCRIPTIONS table entries
            if (doUpdate(sub)) {
                // send response
                elr.setResult(HttpServletResponse.SC_OK);
                eventlogger.info(elr.toString());
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType(SUBFULL_CONTENT_TYPE);
                try {
                    resp.getOutputStream().print(sub.asLimitedJSONObject().toString());
                } catch (IOException ioe) {
                    eventlogger.error("PROV0192 SubscriptionServlet.doPut: " + ioe.getMessage(), ioe);
                }

                /**Change Owner ship of Subscriber     Adding for group feature:Rally US708115*/
                if (jo.has("changeowner") && subjectgroup != null) {
                    try {
                        Boolean changeowner = (Boolean) jo.get("changeowner");
                        if (changeowner != null && changeowner.equals(true)) {
                            sub.setSubscriber(req.getHeader(BEHALF_HEADER));
                            sub.changeOwnerShip();
                        }
                    } catch (JSONException je) {
                        eventlogger.error("PROV0193 SubscriptionServlet.doPut: " + je.getMessage(), je);
                    }
                }
                /***End of change ownership*/

                provisioningDataChanged();
            } else {
                // Something went wrong with the UPDATE
                elr.setResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, DB_PROBLEM_MSG, intlogger);
            }
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * POST on the &lt;subscriptionUrl&gt; -- control a subscription. See the <i>Resetting a Subscription's Retry
     * Schedule</i> section in the <b>Provisioning API</b> document for details on how this method should be invoked.
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {

        setIpFqdnRequestIDandInvocationIDForEelf("doPost", req);
        eelfLogger.info(EelfMsgs.ENTRY);
        try {
            eelfLogger.info(EelfMsgs.MESSAGE_WITH_BEHALF, req.getHeader(BEHALF_HEADER));
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
                super.doPost(req, resp);
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
            final int subid = getIdFromPath(req);
            if (subid < 0 || Subscription.getSubscriptionById(subid) == null) {
                message = BAD_SUB;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            // check content type is SUBCNTRL_CONTENT_TYPE, version 1.0
            ContentHeader ch = getContentHeader(req);
            String ver = ch.getAttribute("version");
            if (!ch.getType().equals(SUBCNTRL_CONTENT_TYPE) || !"1.0".equals(ver)) {
                message = "Incorrect content-type";
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, message, eventlogger);
                return;
            }
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
            JSONObject jo = getJSONfromInput(req);
            if (jo == null) {
                message = BAD_JSON;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString());
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
                return;
            }
            try {
                // Only the active POD sends notifications
                boolean active = SynchronizerTask.getSynchronizer().isActive();
                boolean bool = jo.getBoolean("failed");
                if (active && !bool) {
                    // Notify all nodes to reset the subscription
                    SubscriberNotifyThread thread = new SubscriberNotifyThread();
                    thread.resetSubscription(subid);
                    thread.start();
                }
                // send response
                elr.setResult(HttpServletResponse.SC_ACCEPTED);
                eventlogger.info(elr.toString());
                resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            } catch (JSONException e) {
                message = BAD_JSON;
                elr.setMessage(message);
                elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                eventlogger.error(elr.toString(), e);
                sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, message, eventlogger);
            }
        } finally {
            eelfLogger.info(EelfMsgs.EXIT);
        }
    }

    /**
     * A Thread class used to serially send reset notifications to all nodes in the DR network, when a POST is received
     * for a subscription.
     */
    public static class SubscriberNotifyThread extends Thread {

        static final String URL_TEMPLATE = "http://%s/internal/resetSubscription/%d";
        private List<String> urls = new ArrayList<>();

        SubscriberNotifyThread() {
            setName("SubscriberNotifyThread");
        }

        void resetSubscription(int subid) {
            for (String nodename : BaseServlet.getNodes()) {
                String url = String.format(URL_TEMPLATE, nodename, subid);
                urls.add(url);
            }
        }

        @Override
        public void run() {
            try {
                while (!urls.isEmpty()) {
                    String url = urls.remove(0);
                    forceGetThrough(url);
                }
            } catch (Exception e) {
                intlogger.warn("PROV0195 Caught exception in SubscriberNotifyThread: " + e.getMessage(), e);
            }
        }

        private void forceGetThrough(String url) {
            try {
                URL urlObj = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                conn.connect();
                conn.getContentLength();    // Force the GET through
                conn.disconnect();
            } catch (IOException e) {
                intlogger.info("PROV0194 Error accessing URL: " + url + ": " + e.getMessage(), e);
            }
        }
    }
}
