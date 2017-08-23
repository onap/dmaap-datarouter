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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONObject;

/**
 * The representation of Subscription delivery information.  This includes the URL to deliver to,
 * login and password, and whether to use the "HTTP 100-continue" feature for this subscription.
 * @author Robert Eby
 * @version $Id: SubDelivery.java,v 1.2 2013/06/20 14:11:05 eby Exp $
 */
public class SubDelivery implements JSONable {
	private String url;
	private String user;
	private String password;
	private boolean use100;

	public SubDelivery() {
		this("", "", "", false);
	}
	public SubDelivery(String url, String user, String password, boolean use100) {
		this.url      = url;
		this.user     = user;
		this.password = password;
		this.use100   = use100;
	}
	public SubDelivery(ResultSet rs) throws SQLException {
		this.url      = rs.getString("DELIVERY_URL");
		this.user     = rs.getString("DELIVERY_USER");
		this.password = rs.getString("DELIVERY_PASSWORD");
		this.use100   = rs.getBoolean("DELIVERY_USE100");

	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isUse100() {
		return use100;
	}
	public void setUse100(boolean use100) {
		this.use100 = use100;
	}
	@Override
	public JSONObject asJSONObject() {
		JSONObject jo = new JSONObject();
		jo.put("url", url);
		jo.put("user", user);
		jo.put("password", password);
		jo.put("use100", use100);
		return jo;
	}
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SubDelivery))
			return false;
		SubDelivery os = (SubDelivery) obj;
		if (!url.equals(os.url))
			return false;
		if (!user.equals(os.user))
			return false;
		if (!password.equals(os.password))
			return false;
		if (use100 != os.use100)
			return false;
		return true;
	}
}
