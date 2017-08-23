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


package org.onap.dmaap.datarouter.provisioning.beans;

import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;

import org.onap.dmaap.datarouter.provisioning.BaseServlet;

/**
 * This class is used to log provisioning server events.  Each event consists of a who
 * (who made the provisioning request including the IP address, the X-ATT-DR-ON-BEHALF-OF
 * header value, and the client certificate), a what (what request was made; the method
 * and servlet involved), and a how (how the request was handled; the result code and
 * message returned to the client).  EventLogRecords are logged using log4j at the INFO level.
 *
 * @author Robert Eby
 * @version $Id: EventLogRecord.java,v 1.1 2013/04/26 21:00:25 eby Exp $
 */
public class EventLogRecord {
	private final String ipaddr;		// Who
	private final String behalfof;
	private final String clientSubject;
	private final String method;		// What
	private final String servlet;
	private int result;					// How
	private String message;

	public EventLogRecord(HttpServletRequest request) {
		// Who is making the request
		this.ipaddr = request.getRemoteAddr();
		String s = request.getHeader(BaseServlet.BEHALF_HEADER);
		this.behalfof = (s != null) ? s : "";
		X509Certificate certs[] = (X509Certificate[]) request.getAttribute(BaseServlet.CERT_ATTRIBUTE);
		this.clientSubject = (certs != null && certs.length > 0)
			? certs[0].getSubjectX500Principal().getName() : "";

		// What is the request
		this.method  = request.getMethod();
		this.servlet = request.getServletPath();

		// How was it dealt with
		this.result = -1;
		this.message = "";
	}
	public void setResult(int result) {
		this.result = result;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	@Override
	public String toString() {
		return String.format(
			"%s %s \"%s\" %s %s %d \"%s\"",
			ipaddr, behalfof, clientSubject,
			method, servlet,
			result, message
		);
	}
}
