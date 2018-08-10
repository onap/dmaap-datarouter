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

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The representation of a Feed authorization.  This encapsulates the authorization information about a feed.
 *
 * @author Robert Eby
 * @version $Id: FeedAuthorization.java,v 1.2 2013/06/20 14:11:05 eby Exp $
 */
public class FeedAuthorization implements JSONable {
    private String classification;
    private Set<FeedEndpointID> endpoint_ids;
    private Set<String> endpoint_addrs;

    public FeedAuthorization() {
        this.classification = "";
        this.endpoint_ids = new HashSet<FeedEndpointID>();
        this.endpoint_addrs = new HashSet<String>();
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public Set<FeedEndpointID> getEndpoint_ids() {
        return endpoint_ids;
    }

    public void setEndpoint_ids(Set<FeedEndpointID> endpoint_ids) {
        this.endpoint_ids = endpoint_ids;
    }

    public Set<String> getEndpoint_addrs() {
        return endpoint_addrs;
    }

    public void setEndpoint_addrs(Set<String> endpoint_addrs) {
        this.endpoint_addrs = endpoint_addrs;
    }

    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put("classification", classification);
        JSONArray ja = new JSONArray();
        for (FeedEndpointID eid : endpoint_ids) {
            ja.put(eid.asJSONObject());
        }
        jo.put("endpoint_ids", ja);
        ja = new JSONArray();
        for (String t : endpoint_addrs) {
            ja.put(t);
        }
        jo.put("endpoint_addrs", ja);
        return jo;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FeedAuthorization))
            return false;
        FeedAuthorization of = (FeedAuthorization) obj;
        if (!classification.equals(of.classification))
            return false;
        if (!endpoint_ids.equals(of.endpoint_ids))
            return false;
        if (!endpoint_addrs.equals(of.endpoint_addrs))
            return false;
        return true;
    }
}
