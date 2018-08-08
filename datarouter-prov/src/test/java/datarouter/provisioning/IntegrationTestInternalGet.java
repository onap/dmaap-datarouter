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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;
import org.onap.dmaap.datarouter.provisioning.beans.Parameters;

public class IntegrationTestInternalGet extends IntegrationTestBase {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testNormal() {
        String url   = props.getProperty("test.host") + "/internal/prov";
        HttpGet httpPost = new HttpGet(url);
        try {
            httpPost.addHeader(FeedServlet.BEHALF_HEADER, "JUnit");

            HttpResponse response = httpclient.execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                fail("Unexpected response, expect " + 200 + " got " + code);
            }

            HttpEntity entity = response.getEntity();
            String ctype = entity.getContentType().getValue().trim();
            boolean ok  = ctype.equals(FeedServlet.PROVFULL_CONTENT_TYPE1);
            ok |= ctype.equals(FeedServlet.PROVFULL_CONTENT_TYPE2);
            if (!ok) {
                fail("Got wrong content type: " + ctype);
            }

            // do something useful with the response body and ensure it is fully consumed
            if (ok) {
                JSONObject jo = null;
                try {
                    jo = new JSONObject(new JSONTokener(entity.getContent()));
                } catch (Exception e) {
                    fail("Bad JSON: " + e.getMessage());
                }
                try {
                    jo.getJSONArray("feeds");
                    jo.getJSONArray("subscriptions");
                    JSONObject jo2 = jo.getJSONObject("parameters");
                    jo2.getJSONArray(Parameters.NODES);
                    jo2.getString(Parameters.ACTIVE_POD);
                    jo2.getString(Parameters.STANDBY_POD);
                    jo2.getInt(Parameters.LOGROLL_INTERVAL);
                    jo2.getInt(Parameters.DELIVERY_INIT_RETRY_INTERVAL);
                    jo2.getInt(Parameters.DELIVERY_MAX_RETRY_INTERVAL);
                    jo2.getInt(Parameters.DELIVERY_RETRY_RATIO);
                    jo2.getInt(Parameters.DELIVERY_MAX_AGE);
                } catch (JSONException e) {
                    fail("required field missing from result: " + e.getMessage());
                }
            } else {
                EntityUtils.consume(entity);
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            httpPost.releaseConnection();
        }
    }
}
