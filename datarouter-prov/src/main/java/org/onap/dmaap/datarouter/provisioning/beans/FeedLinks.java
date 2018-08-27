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
 * The URLs associated with a Feed.
 *
 * @author Robert Eby
 * @version $Id: FeedLinks.java,v 1.3 2013/07/05 13:48:05 eby Exp $
 */
public class FeedLinks implements JSONable {
    private String self;
    private String publish;
    private String subscribe;
    private String log;

    public FeedLinks() {
        self = publish = subscribe = log = null;
    }

    public FeedLinks(JSONObject jo) throws InvalidObjectException {
        this();
        self = jo.getString("self");
        publish = jo.getString("publish");
        subscribe = jo.getString("subscribe");
        log = jo.getString("log");
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getPublish() {
        return publish;
    }

    public void setPublish(String publish) {
        this.publish = publish;
    }

    public String getSubscribe() {
        return subscribe;
    }

    public void setSubscribe(String subscribe) {
        this.subscribe = subscribe;
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
        jo.put("publish", publish);
        jo.put("subscribe", subscribe);
        jo.put("log", log);
        return jo;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FeedLinks))
            return false;
        FeedLinks of = (FeedLinks) obj;
        if (!self.equals(of.self))
            return false;
        if (!publish.equals(of.publish))
            return false;
        if (!subscribe.equals(of.subscribe))
            return false;
        if (!log.equals(of.log))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(self, publish, subscribe, log);
    }
}
