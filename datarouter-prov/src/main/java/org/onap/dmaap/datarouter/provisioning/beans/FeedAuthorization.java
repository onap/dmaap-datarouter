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
import java.util.Objects;
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
    private Set<FeedEndpointID> endpointIds;
    private Set<String> endpointAddrs;

    /**
     * FeedAuthoization constructor.
     */
    public FeedAuthorization() {
        this.classification = "";
        this.endpointIds = new HashSet<>();
        this.endpointAddrs = new HashSet<>();
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public Set<FeedEndpointID> getEndpointIDS() {
        return endpointIds;
    }

    public void setEndpointIDS(Set<FeedEndpointID> endpointIds) {
        this.endpointIds = endpointIds;
    }

    public Set<String> getEndpointAddrs() {
        return endpointAddrs;
    }

    public void setEndpointAddrs(Set<String> endpointAddrs) {
        this.endpointAddrs = endpointAddrs;
    }

    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put("classification", classification);
        JSONArray ja = new JSONArray();
        for (FeedEndpointID eid : endpointIds) {
            ja.put(eid.asJSONObject());
        }
        jo.put("endpoint_ids", ja);
        ja = new JSONArray();
        for (String t : endpointAddrs) {
            ja.put(t);
        }
        jo.put("endpoint_addrs", ja);
        return jo;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FeedAuthorization)) {
            return false;
        }
        FeedAuthorization of = (FeedAuthorization) obj;
        if (!classification.equals(of.classification)) {
            return false;
        }
        if (!endpointIds.equals(of.endpointIds)) {
            return false;
        }
        if (!endpointAddrs.equals(of.endpointAddrs)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classification, endpointIds, endpointAddrs);
    }
}
