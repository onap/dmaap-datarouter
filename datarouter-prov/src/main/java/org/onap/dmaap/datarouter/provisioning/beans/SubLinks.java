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

import java.io.InvalidObjectException;
import java.util.Objects;

import org.json.JSONObject;

/**
 * The URLs associated with a Subscription.
 *
 * @author Robert Eby
 * @version $Id: SubLinks.java,v 1.3 2013/07/05 13:48:05 eby Exp $
 */
public class SubLinks implements JSONable {
    private String self;
    private String feed;
    private String log;

    public SubLinks() {
        self = feed = log = null;
    }

    public SubLinks(JSONObject jo) throws InvalidObjectException {
        this();
        self = jo.getString("self");
        feed = jo.getString("feed");
        log = jo.getString("log");
    }

    public SubLinks(String self, String feed, String log) {
        this.self = self;
        this.feed = feed;
        this.log = log;
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getFeed() {
        return feed;
    }

    public void setFeed(String feed) {
        this.feed = feed;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put("self", self);
        jo.put("feed", feed);
        jo.put("log", log);
        return jo;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SubLinks))
            return false;
        SubLinks os = (SubLinks) obj;
        if (!self.equals(os.self))
            return false;
        if (!feed.equals(os.feed))
            return false;
        if (!log.equals(os.log))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(self, feed, log);
    }
}
