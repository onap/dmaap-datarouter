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

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;
import org.onap.dmaap.datarouter.provisioning.SubscribeServlet;

public class IntegrationTestSubscribePost extends IntegrationTestBase {
    private int feednum = 0;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * This is the setUp method.
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        getDBstate();
        // use the first feed to subscribe to
        JSONArray ja = db_state.getJSONArray("feeds");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject feed0 = ja.getJSONObject(i);
            if (feed0 != null && !feed0.getBoolean("deleted")) {
                feednum = feed0.getInt("feedid");
                return;
            }
        }
    }

    @Test
    public void testNormal() {
        JSONObject jo = buildSubRequest();
        testCommon(jo, HttpServletResponse.SC_CREATED);
    }

    @Test
    public void testMissingUrl() {
        JSONObject jo = buildSubRequest();
        jo.getJSONObject("delivery").remove("url");
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testTooLongUrl() {
        JSONObject jo = buildSubRequest();
        jo.getJSONObject("delivery").put("url", "https://" + s257);
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testMissingUser() {
        JSONObject jo = buildSubRequest();
        jo.getJSONObject("delivery").remove("user");
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testTooLongUser() {
        JSONObject jo = buildSubRequest();
        jo.getJSONObject("delivery").put("user", s33);
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testMissingPassword() {
        JSONObject jo = buildSubRequest();
        jo.getJSONObject("delivery").remove("password");
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testTooLongPassword() {
        JSONObject jo = buildSubRequest();
        jo.getJSONObject("delivery").put("password", s33);
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testNonBooleanMetadata() {
        JSONObject jo = buildSubRequest();
        jo.put("metadataOnly", s33);
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST);
    }

    private void testCommon(JSONObject jo, int expect) {
        String url   = props.getProperty("test.host") + "/subscribe/" + feednum;
        HttpPost httpPost = new HttpPost(url);
        try {
            httpPost.addHeader(SubscribeServlet.BEHALF_HEADER, "JUnit");
            String strJo = jo.toString();
            HttpEntity body = new ByteArrayEntity(strJo.getBytes(),
                    ContentType.create(SubscribeServlet.SUB_CONTENT_TYPE));
            httpPost.setEntity(body);

            HttpResponse response = httpclient.execute(httpPost);
            ckResponse(response, expect);

            HttpEntity entity = response.getEntity();
            String ctype = entity.getContentType().getValue();
            int code = response.getStatusLine().getStatusCode();
            if (code == HttpServletResponse.SC_CREATED && !ctype.equals(SubscribeServlet.SUBFULL_CONTENT_TYPE)) {
                fail("Got wrong content type: " + ctype);
            }

            // do something useful with the response body and ensure it is fully consumed
            if (ctype.equals(FeedServlet.SUBFULL_CONTENT_TYPE)) {
                JSONObject jo2 = null;
                try {
                    jo2 = new JSONObject(new JSONTokener(entity.getContent()));
                } catch (Exception e) {
                    fail("Bad JSON: " + e.getMessage());
                }
                try {
                    jo2.getString("subscriber");
                    JSONObject jo3 = jo2.getJSONObject("links");
                    jo3.getString("self");
                    jo3.getString("feed");
                    jo3.getString("log");
                } catch (JSONException e) {
                    fail("required field missing from result: " + e.getMessage());
                }
            } else {
                EntityUtils.consume(entity);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            httpPost.releaseConnection();
        }
    }

    private JSONObject buildSubRequest() {
        JSONObject jo2 = new JSONObject();
        jo2.put("url", "https://www.att.com/");
        jo2.put("user", "dmr");
        jo2.put("password", "passw0rd");
        jo2.put("use100", true);

        JSONObject jo = new JSONObject();
        jo.put("delivery", jo2);
        jo.put("metadataOnly", Boolean.FALSE);
        return jo;
    }
}
