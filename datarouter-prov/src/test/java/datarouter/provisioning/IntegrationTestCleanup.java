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

package datarouter.provisioning;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;

public class IntegrationTestCleanup extends IntegrationTestBase {
    @Before
    public void setUp() throws Exception {
        super.setUp();
        getDBstate();
    }

    @Test
    public void testNormal() {
        // Delete all feeds w/JUnit as publisher
        JSONArray ja = db_state.getJSONArray("feeds");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject feed = ja.getJSONObject(i);
            if (feed != null && !feed.getBoolean("deleted")) {
                if (feed.getString("publisher").equals("JUnit")) {
                    int feedid = feed.getInt("feedid");
                    delete("/feed/" + feedid);
                }
            }
        }
        // Delete all subscriptions w/JUnit as subscriber
        ja = db_state.getJSONArray("subscriptions");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject sub = ja.getJSONObject(i);
            if (sub != null && sub.getString("subscriber").equals("JUnit")) {
                int subid = sub.getInt("subid");
                delete("/subs/" + subid);
            }
        }
    }

    private void delete(String uri) {
        String url = props.getProperty("test.host") + uri;;
        HttpDelete del = new HttpDelete(url);
        try {
            del.addHeader(FeedServlet.BEHALF_HEADER, "JUnit");
            HttpResponse response = httpclient.execute(del);
            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            del.releaseConnection();
        }
    }
}
