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

import com.att.research.datarouter.authz.AuthorizationResponse;
import com.att.research.datarouter.provisioning.BaseServlet.ContentHeader;
import com.att.research.datarouter.provisioning.beans.EventLogRecord;
import com.att.research.datarouter.provisioning.beans.Group;
import com.att.research.datarouter.provisioning.beans.Subscription;
import com.att.research.datarouter.provisioning.utils.JSONUtilities;

/**
 * This servlet handles provisioning for the &lt;groups&gt; which is generated by the provisioning
 * server to handle the creation and inspection of groups for FEEDS and SUBSCRIPTIONS.
 *
 * @author Vikram Singh
 * @version $Id$
 * @version $Id: Group.java,v 1.0 2016/07/19
 */
@SuppressWarnings("serial")
public class GroupServlet extends ProxyServlet {
	/**
	 * DELETE on the &lt;GRUPS&gt; -- not supported.
	 */
	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String message = "DELETE not allowed for the GROUPS.";
		EventLogRecord elr = new EventLogRecord(req);
		elr.setMessage(message);
		elr.setResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		eventlogger.info(elr);
		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);
	}
	/**
	 * GET on the the list of groups to a feed/sub.
	 * See the <i>Groups Collection Query</i> section in the <b>Provisioning API</b>
	 * document for details on how this method should be invoked.
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
		
		// Check with the Authorizer
		/*AuthorizationResponse aresp = authz.decide(req);
		if (! aresp.isAuthorized()) {
			message = "Policy Engine disallows access.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_FORBIDDEN);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
			return;
		}*/
		
		
		/*ContentHeader ch = getContentHeader(req);
		String ver = ch.getAttribute("version");
		if (!ch.getType().equals(GROUPLIST_CONTENT_TYPE) || !(ver.equals("1.0") || ver.equals("2.0"))) {
			intlogger.debug("Content-type is: "+req.getHeader("Content-Type"));
			message = "Incorrect content-type";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, message);
			return;
		}*/
		
		
		int groupid = getIdFromPath(req);
		if (groupid < 0) {
			message = "Missing or bad group number.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return;
		}
			
		Group gup = Group.getGroupById(groupid);
		// send response
		elr.setResult(HttpServletResponse.SC_OK);
		eventlogger.info(elr);
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType(GROUPFULL_CONTENT_TYPE);
		resp.getOutputStream().print(gup.asJSONObject().toString());

		// Display a list of Groups
		/*Collection<Group> list = Group.getGroupById(groupid);
		String t = JSONUtilities.createJSONArray(list);

		// send response
		elr.setResult(HttpServletResponse.SC_OK);
		eventlogger.info(elr);
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType(GROUPLIST_CONTENT_TYPE);
		resp.getOutputStream().print(t);*/
	}
	/**
	 * PUT on the &lt;GROUPS&gt; -- not supported.
	 */
	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
			super.doPut(req, resp);
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
		int groupid = getIdFromPath(req);
		if (groupid < 0) {
			message = "Missing or bad groupid.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return;
		}
		Group oldgup = Group.getGroupById(groupid);
		if (oldgup == null) {
			message = "Missing or bad group number.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_NOT_FOUND);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, message);
			return;
		}
		// Check with the Authorizer
		/*AuthorizationResponse aresp = authz.decide(req);
		if (! aresp.isAuthorized()) {
			message = "Policy Engine disallows access.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_FORBIDDEN);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
			return;
		}*/
		// check content type is SUB_CONTENT_TYPE, version 1.0
		ContentHeader ch = getContentHeader(req);
		String ver = ch.getAttribute("version");
		if (!ch.getType().equals(GROUP_BASECONTENT_TYPE) || !(ver.equals("1.0") || ver.equals("2.0"))) {
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
		Group gup = null;
		try {
			gup = new Group(jo);
		} catch (InvalidObjectException e) {
			message = e.getMessage();
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return;
		}
		gup.setGroupid(oldgup.getGroupid());
	
		
		Group gb2 = Group.getGroupMatching(gup, oldgup.getGroupid());
		if (gb2 != null) {
			eventlogger.warn("PROV0011 Creating a duplicate Group: "+gup.getName());
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Duplicate Group:"+gup.getName());
			return;
		}
		
		// Update Groups table entries
		if (doUpdate(gup)) {
			// send response
			elr.setResult(HttpServletResponse.SC_OK);
			eventlogger.info(elr);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType(GROUPFULL_CONTENT_TYPE);
			resp.getOutputStream().print(gup.asJSONObject().toString());
			provisioningDataChanged();
		} else {
			// Something went wrong with the UPDATE
			elr.setResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, DB_PROBLEM_MSG);
		}
	}
	/**
	 * POST on the &lt;groups&gt; -- create a new GROUPS to a feed.
	 * See the <i>Creating a GROUPS</i> section in the <b>Provisioning API</b>
	 * document for details on how this method should be invoked.
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
		/*int feedid = getIdFromPath(req);
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
		}*/
		// Check with the Authorizer
		/*AuthorizationResponse aresp = authz.decide(req);
		if (! aresp.isAuthorized()) {
			message = "Policy Engine disallows access.";
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_FORBIDDEN);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, message);
			return;
		}*/

		// check content type is SUB_CONTENT_TYPE, version 1.0
		ContentHeader ch = getContentHeader(req);
		String ver = ch.getAttribute("version");
		if (!ch.getType().equals(GROUP_BASECONTENT_TYPE) || !(ver.equals("1.0") || ver.equals("2.0"))) {
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
		
		Group gup = null;
		try {
			gup = new Group(jo);
		} catch (InvalidObjectException e) {
			message = e.getMessage();
			elr.setMessage(message);
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return;
		}
		//gup.setFeedid(feedid);
		//sub.setSubscriber(bhdr);	// set from X-ATT-DR-ON-BEHALF-OF header

		// Check if this group already exists; not an error (yet), just warn
		Group gb2 = Group.getGroupMatching(gup);
		if (gb2 != null) {
			eventlogger.warn("PROV0011 Creating a duplicate Group: "+gup.getName());
			elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Duplicate Group:"+gup.getName());
			return;
		}
		
		
		// Create GROUPS table entries
		if (doInsert(gup)) {
			// send response
			elr.setResult(HttpServletResponse.SC_CREATED);
			eventlogger.info(elr);
			resp.setStatus(HttpServletResponse.SC_CREATED);
			resp.setContentType(GROUPFULL_CONTENT_TYPE);
			resp.getOutputStream().print(gup.asJSONObject().toString());
			provisioningDataChanged();
		} else {
			// Something went wrong with the INSERT
			elr.setResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			eventlogger.info(elr);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, DB_PROBLEM_MSG);
		}
	}
}
