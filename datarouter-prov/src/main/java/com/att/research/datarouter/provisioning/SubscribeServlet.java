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


package com.att.research.datarouter.provisioning;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.att.research.datarouter.authz.AuthorizationResponse;
import com.att.research.datarouter.provisioning.beans.EventLogRecord;
import com.att.research.datarouter.provisioning.beans.Feed;
import com.att.research.datarouter.provisioning.beans.Subscription;
import com.att.research.datarouter.provisioning.eelf.EelfMsgs;
import com.att.research.datarouter.provisioning.utils.JSONUtilities;

/**
 * This servlet handles provisioning for the &lt;subscribeURL&gt; which is generated by the provisioning
 * server to handle the creation and inspection of subscriptions to a specific feed.
 *
 * @author Robert Eby
 * @version $Id$
 */
@SuppressWarnings("serial")
public class SubscribeServlet extends ProxyServlet {
	
	//Adding EELF Logger Rally:US664892  
    private static EELFLogger eelflogger = EELFManager.getInstance().getLogger("com.att.research.datarouter.provisioning.SubscribeServlet");

	/**
	 * DELETE on the &lt;subscribeUrl&gt; -- not supported.
	 */
	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setIpAndFqdnForEelf("doDelete");
		eelflogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_SUBID, req.getHeader(BEHALF_HEADER),getIdFromPath(req)+"");
		String message = "DELETE not allowed for the subscribeURL.";
		EventLogRecord elr = new EventLogRecord(req);
		elr.setMessage(message);
		elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		eventlogger.info(elr);
		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);
	}
	/**
	 * GET on the &lt;subscribeUrl&gt; -- get the list of subscriptions to a feed.
	 * See the <i>Subscription Collection Query</i> section in the <b>Provisioning API</b>
	 * document for details on how this method should be invoked.
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setIpAndFqdnForEelf("doGet");
		eelflogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_SUBID, req.getHeader(BEHALF_HEADER),getIdFromPath(req)+"");
		EventLogRecord elr = new EventLogRecord(req);
		String message = isAuthorizedForProvisioning(req);
		if (message != null) {
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_FORBIDDEN);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
			return;
		}
		if (isProxyServer()) {
			super.doGet(req, resp);
			return;
		}
		String bhdr = req.getHeader(BEHALF_HEADER);
		if (bhdr == null) {
			message = "Missing "+BEHALF_HEADER+" header.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return;
		}
		int feedid = getIdFromPath(req);
		if (feedid < 0) {
			message = "Missing or bad feed number.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return;
		}
		Feed feed = Feed.getFeedById(feedid);
		if (feed == null || feed.isDeleted()) {
			message = "Missing or bad feed number.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_NOT_FOUND);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, message);
			return;
		}
		// Check with the Authorizer
		AuthorizationResponse aresp = authz.decide(req);
		if (! aresp.isAuthorized()) {
			message = "Policy Engine disallows access.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_FORBIDDEN);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
			return;
		}

		// Display a list of URLs
		Collection<String> list = Subscription.getSubscriptionUrlList(feedid);
		String t = JSONUtilities.createJSONArray(list);

		// send response
		elr.setResult(HttpServletResponse.SC_OK);
		eventlogger.info(elr);
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType(SUBLIST_CONTENT_TYPE);
		resp.getOutputStream().print(t);
	}
	/**
	 * PUT on the &lt;subscribeUrl&gt; -- not supported.
	 */
	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setIpAndFqdnForEelf("doPut");
		eelflogger.info(EelfMsgs.MESSAGE_WITH_BEHALF_AND_SUBID, req.getHeader(BEHALF_HEADER),getIdFromPath(req)+"");
		String message = "PUT not allowed for the subscribeURL.";
		EventLogRecord elr = new EventLogRecord(req);
		elr.setMessage(message);
		elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		eventlogger.info(elr);
		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);
	}
	/**
	 * POST on the &lt;subscribeUrl&gt; -- create a new subscription to a feed.
	 * See the <i>Creating a Subscription</i> section in the <b>Provisioning API</b>
	 * document for details on how this method should be invoked.
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setIpAndFqdnForEelf("doPost");
		eelflogger.info(EelfMsgs.MESSAGE_WITH_BEHALF, req.getHeader(BEHALF_HEADER));
		EventLogRecord elr = new EventLogRecord(req);
		String message = isAuthorizedForProvisioning(req);
		if (message != null) {
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_FORBIDDEN);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
			return;
		}
		if (isProxyServer()) {
			super.doPost(req, resp);
			return;
		}
		String bhdr = req.getHeader(BEHALF_HEADER);
		if (bhdr == null) {
			message = "Missing "+BEHALF_HEADER+" header.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return;
		}
		int feedid = getIdFromPath(req);
		if (feedid < 0) {
			message = "Missing or bad feed number.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return;
		}
		Feed feed = Feed.getFeedById(feedid);
		if (feed == null || feed.isDeleted()) {
			message = "Missing or bad feed number.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_NOT_FOUND);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, message);
			return;
		}
		// Check with the Authorizer
		AuthorizationResponse aresp = authz.decide(req);
		if (! aresp.isAuthorized()) {
			message = "Policy Engine disallows access.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_FORBIDDEN);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
			return;
		}

		// check content type is SUB_CONTENT_TYPE, version 1.0
		ContentHeader ch = getContentHeader(req);
		String ver = ch.getAttribute("version");
		if (!ch.getType().equals(SUB_BASECONTENT_TYPE) || !(ver.equals("1.0") || ver.equals("2.0"))) {
			intlogger.debug("Content-type is: "+req.getHeader("Content-Type"));
			message = "Incorrect content-type";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, message);
			return;
		}
		JSONObject jo = getJSONfromInput(req);
		if (jo == null) {
			message = "Badly formed JSON";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return;
		}
		if (intlogger.isDebugEnabled())
			intlogger.debug(jo.toString());
		if (++active_subs > max_subs) {
			active_subs--;
			message = "Cannot create subscription; the maximum number of subscriptions has been configured.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_CONFLICT);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_CONFLICT, message);
			return;
		}
		Subscription sub = null;
		try {
			sub = new Subscription(jo);
		} catch (InvalidObjectException e) {
			active_subs--;
			message = e.getMessage();
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return;
		}
		sub.setFeedid(feedid);
		sub.setSubscriber(bhdr);	// set from X-ATT-DR-ON-BEHALF-OF header

		// Check if this subscription already exists; not an error (yet), just warn
		Subscription sub2 = Subscription.getSubscriptionMatching(sub);
		if (sub2 != null)
			intlogger.warn("PROV0011 Creating a duplicate subscription: new subid="+sub.getSubid()+", old subid="+sub2.getSubid());

		// Create SUBSCRIPTIONS table entries
		if (doInsert(sub)) {
			// send response
			elr.setResult(HttpServletResponse.SC_CREATED);
			eventlogger.info(elr);
			resp.setStatus(HttpServletResponse.SC_CREATED);
			resp.setContentType(SUBFULL_CONTENT_TYPE);
			resp.setHeader("Location", sub.getLinks().getSelf());
			resp.getOutputStream().print(sub.asLimitedJSONObject().toString());

			provisioningDataChanged();
		} else {
			// Something went wrong with the INSERT
			active_subs--;
			elr.setResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, DB_PROBLEM_MSG);
		}
	}
}
