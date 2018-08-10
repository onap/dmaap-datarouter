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
 * The representation of a Feed endpoint.  This contains a login/password pair.
 *
 * @author Robert Eby
 * @version $Id: FeedEndpointID.java,v 1.1 2013/04/26 21:00:26 eby Exp $
 */
public class FeedEndpointID implements JSONable {
    private String id;
    private String password;

    public FeedEndpointID() {
        this("", "");
    }

    public FeedEndpointID(String id, String password) {
        this.id = id;
        this.password = password;
    }

    public FeedEndpointID(ResultSet rs) throws SQLException {
        this.id = rs.getString("USERID");
        this.password = rs.getString("PASSWORD");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put("id", id);
        jo.put("password", password);
        return jo;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FeedEndpointID))
            return false;
        FeedEndpointID f2 = (FeedEndpointID) obj;
        return id.equals(f2.id) && password.equals(f2.password);
    }

    @Override
    public int hashCode() {
        return (id + ":" + password).hashCode();
    }
}
