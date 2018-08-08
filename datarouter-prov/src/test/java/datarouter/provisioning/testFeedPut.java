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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
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

public class testFeedPut extends testBase {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        getDBstate();
    }

    @Test
    public void testPutNoFeedID() {
        JSONObject jo = buildFeedRequest();
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST, "/feed/");
    }
    @Test
    public void testPutNoFeed() {
        JSONObject jo = buildFeedRequest();
        testCommon(jo, HttpServletResponse.SC_NOT_FOUND, "/feed/999999");
    }
    @Test
    public void testBadContentType() {
        JSONObject jo = buildFeedRequest();
        testCommon(jo, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "bad/bad", "JUnit");
    }
    @Test
    public void testChangeName() {
        JSONObject jo = buildFeedRequest();
        jo.put("name", "badname");
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST, FeedServlet.FEED_CONTENT_TYPE, "JUnit");
    }
    @Test
    public void testChangeVersion() {
        JSONObject jo = buildFeedRequest();
        jo.put("version", "badvers");
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST, FeedServlet.FEED_CONTENT_TYPE, "JUnit");
    }
    @Test
    public void testBadPublisher() {
        JSONObject jo = buildFeedRequest();
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST, FeedServlet.FEED_CONTENT_TYPE, "BadBadBad");
    }
    @Test
    public void testChangeDescription() {
        JSONObject jo = buildFeedRequest();
        // change descr
        jo.put("description", "This description HAS BEEN CHANGED!!!");
        testCommon(jo, HttpServletResponse.SC_OK, FeedServlet.FEED_CONTENT_TYPE, "JUnit");
    }

    private void testCommon(JSONObject jo, int expect, String uri) {
        testCommon(jo, expect, FeedServlet.FEED_CONTENT_TYPE, "Junit", uri);
    }
    private void testCommon(JSONObject jo, int expect, String ctype, String bhdr) {
        JSONArray ja = db_state.getJSONArray("feeds");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject feed0 = ja.getJSONObject(i);
            if (!feed0.getBoolean("deleted") && feed0.getString("publisher").equals(bhdr)) {
                int feedid = feed0.getInt("feedid");
                testCommon(jo, expect, ctype, bhdr, "/feed/"+feedid);
                return;
            }
        }
    }
    private void testCommon(JSONObject jo, int expect, String ctype, String bhdr, String uri) {
        String url   = props.getProperty("test.host") + uri;
        HttpPut put = new HttpPut(url);
        try {
            if (bhdr != null)
                put.addHeader(FeedServlet.BEHALF_HEADER, bhdr);
            String t = jo.toString();
            HttpEntity body = new ByteArrayEntity(t.getBytes(), ContentType.create(ctype));
            put.setEntity(body);

            HttpResponse response = httpclient.execute(put);
            ckResponse(response, expect);

            HttpEntity entity = response.getEntity();
            ctype = entity.getContentType().getValue().trim();
            int code = response.getStatusLine().getStatusCode();
            if (code == HttpServletResponse.SC_CREATED && !ctype.equals(FeedServlet.FEEDFULL_CONTENT_TYPE))
                fail("Got wrong content type: "+ctype);

            if (code == HttpServletResponse.SC_CREATED) {
                Header[] loc = response.getHeaders("Location");
                if (loc == null)
                    fail("Missing Location header.");
            }

            // do something useful with the response body and ensure it is fully consumed
            if (ctype.equals(FeedServlet.FEEDFULL_CONTENT_TYPE)) {
                // ck Location header!
                JSONObject jo2 = null;
                try {
                    jo2 = new JSONObject(new JSONTokener(entity.getContent()));
    System.err.println(jo2.toString());
                } catch (Exception e) {
                    fail("Bad JSON: "+e.getMessage());
                }
                try {
                    jo2.getString("publisher");
                    JSONObject jo3 = jo2.getJSONObject("links");
                    jo3.getString("self");
                    jo3.getString("publish");
                    jo3.getString("subscribe");
                    jo3.getString("log");
                } catch (JSONException e) {
                    fail("required field missing from result: "+e.getMessage());
                }
            } else {
                EntityUtils.consume(entity);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            put.releaseConnection();
        }
    }
    private JSONObject buildFeedRequest() {
        JSONObject jo = new JSONObject();
        jo.put("name", "feed");
        jo.put("version", "1.0.0");
        jo.put("description", "Sample feed used by JUnit to test");

            JSONObject jo2 = new JSONObject();
            jo2.put("classification", "unrestricted");

            JSONArray ja = new JSONArray();
                JSONObject jo3 = new JSONObject();
                jo3.put("id", "id001");
                jo3.put("password", "re1kwelj");
                JSONObject jo4 = new JSONObject();
                jo4.put("id", "id002");
                jo4.put("password", "o9eqlmbd");
                ja.put(jo3);
                ja.put(jo4);
            jo2.put("endpoint_ids", ja);

            ja = new JSONArray();
                ja.put("20.0.0.1");
                ja.put("195.68.12.15");
                ja.put("135.207.136.128/25");
            jo2.put("endpoint_addrs", ja);

        jo.put("authorization", jo2);
        return jo;
    }
}
