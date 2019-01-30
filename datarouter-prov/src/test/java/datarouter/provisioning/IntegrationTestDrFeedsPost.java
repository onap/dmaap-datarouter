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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;

public class IntegrationTestDrFeedsPost extends IntegrationTestBase {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testNormal() {
        JSONObject jo = buildFeedRequest();
        testCommon(jo, HttpServletResponse.SC_CREATED);
    }

    @Test
    public void testNormalNoCtVersion() {
        JSONObject jo = buildFeedRequest();
        testCommon(jo, HttpServletResponse.SC_CREATED, "application/vnd.dr.feed", "JUnit");
    }

    @Test
    public void testBadContentType() {
        JSONObject jo = buildFeedRequest();
        testCommon(jo, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "bad/bad", "Junit");
    }

    @Test
    public void testNoBehalfHeader() {
        JSONObject jo = buildFeedRequest();
        testCommon(jo, HttpServletResponse.SC_BAD_REQUEST, FeedServlet.FEED_CONTENT_TYPE, null);
    }

    @Test
    public void testMissingName() {
        JSONObject jo = buildFeedRequest();
        jo.remove("name");
        testCommon(jo, 400);
    }

    @Test
    public void testTooLongName() {
        JSONObject jo = buildFeedRequest();
        jo.put("name", "123456789012345678901234567890");
        testCommon(jo, 400);
    }

    @Test
    public void testMissingVersion() {
        JSONObject jo = buildFeedRequest();
        jo.remove("version");
        testCommon(jo, 400);
    }

    @Test
    public void testTooLongVersion() {
        JSONObject jo = buildFeedRequest();
        jo.put("version", "123456789012345678901234567890");
        testCommon(jo, 400);
    }

    @Test
    public void testTooLongDescription() {
        // normal request
        JSONObject jo = buildFeedRequest();
        jo.put("description", s257);
        testCommon(jo, 400);
    }

    @Test
    public void testMissingAuthorization() {
        JSONObject jo = buildFeedRequest();
        jo.remove("authorization");
        testCommon(jo, 400);
    }

    @Test
    public void testMissingClassification() {
        JSONObject jo = buildFeedRequest();
        JSONObject j2 = jo.getJSONObject("authorization");
        j2.remove("classification");
        testCommon(jo, 400);
    }

    @Test
    public void testTooLongClassification() {
        JSONObject jo = buildFeedRequest();
        JSONObject j2 = jo.getJSONObject("authorization");
        j2.put("classification", s33);
        testCommon(jo, 400);
    }

    @Test
    public void testNoEndpointIds() {
        JSONObject jo = buildFeedRequest();
        JSONObject j2 = jo.getJSONObject("authorization");
        j2.put("endpoint_ids", new JSONArray());
        testCommon(jo, 400);
    }

    @Test
    public void testBadIpAddress1() {
        JSONObject jo = buildFeedRequest();
        JSONObject j2 = jo.getJSONObject("authorization");
        JSONArray ja = j2.getJSONArray("endpoint_addrs");
        ja.put("ZZZ^&#$%@#&^%$@#&^");
        testCommon(jo, 400);
    }

    @Test
    public void testBadIpAddress2() {
        JSONObject jo = buildFeedRequest();
        JSONObject j2 = jo.getJSONObject("authorization");
        JSONArray ja = j2.getJSONArray("endpoint_addrs");
        ja.put("135.207.136.678");  // bad IPv4 addr
        testCommon(jo, 400);
    }

    @Test
    public void testBadIpAddress3() {
        JSONObject jo = buildFeedRequest();
        JSONObject j2 = jo.getJSONObject("authorization");
        JSONArray ja = j2.getJSONArray("endpoint_addrs");
        ja.put("2001:1890:1110:d000:1a29::17567");  // bad IPv6 addr
        testCommon(jo, 400);
    }

    @Test
    public void testBadNetMask() {
        JSONObject jo = buildFeedRequest();
        JSONObject j2 = jo.getJSONObject("authorization");
        JSONArray ja = j2.getJSONArray("endpoint_addrs");
        ja.put("10.10.10.10/64");
        testCommon(jo, 400);
    }

    @Test
    public void testGoodIpAddress1() {
        JSONObject jo = buildFeedRequest();
        JSONObject j2 = jo.getJSONObject("authorization");
        JSONArray ja = j2.getJSONArray("endpoint_addrs");
        ja.put("135.207.136.175"); // good IPv4 addr
        testCommon(jo, 201);
    }

    @Test
    public void testGoodIpAddress2() {
        JSONObject jo = buildFeedRequest();
        JSONObject j2 = jo.getJSONObject("authorization");
        JSONArray ja = j2.getJSONArray("endpoint_addrs");
        ja.put("2001:1890:1110:d000:1a29::175"); // good IPv6 addr
        testCommon(jo, 201);
    }

    @Test
    public void testGoodNetMask() {
        JSONObject jo = buildFeedRequest();
        JSONObject j2 = jo.getJSONObject("authorization");
        JSONArray ja = j2.getJSONArray("endpoint_addrs");
        ja.put("2001:1890:1110:d000:1a29::175/120");
        testCommon(jo, 201);
    }

    private void testCommon(JSONObject jo, int expect) {
        testCommon(jo, expect, FeedServlet.FEED_CONTENT_TYPE, "JUnit");
    }

    private void testCommon(JSONObject jo, int expect, String ctype, String bhdr) {
        String url   = props.getProperty("test.host") + "/";
        HttpPost httpPost = new HttpPost(url);
        try {
            if (bhdr != null) {
                httpPost.addHeader(FeedServlet.BEHALF_HEADER, bhdr);
            }
            String strJo = jo.toString();
            HttpEntity body = new ByteArrayEntity(strJo.getBytes(), ContentType.create(ctype));
            httpPost.setEntity(body);

            HttpResponse response = httpclient.execute(httpPost);
            ckResponse(response, expect);

            HttpEntity entity = response.getEntity();
            ctype = entity.getContentType().getValue().trim();
            int code = response.getStatusLine().getStatusCode();
            if (code == HttpServletResponse.SC_CREATED && !ctype.equals(FeedServlet.FEEDFULL_CONTENT_TYPE)) {
                fail("Got wrong content type: " + ctype);
            }

            if (code == HttpServletResponse.SC_CREATED) {
                Header[] loc = response.getHeaders("Location");
                if (loc == null) {
                    fail("Missing Location header.");
                }
            }

            // do something useful with the response body and ensure it is fully consumed
            if (ctype.equals(FeedServlet.FEEDFULL_CONTENT_TYPE)) {
                // ck Location header!
                JSONObject jo2 = null;
                try {
                    jo2 = new JSONObject(new JSONTokener(entity.getContent()));
                    System.err.println(jo2.toString());
                } catch (Exception e) {
                    fail("Bad JSON: " + e.getMessage());
                }
                try {
                    jo2.getString("publisher");
                    JSONObject jo3 = jo2.getJSONObject("links");
                    jo3.getString("self");
                    jo3.getString("publish");
                    jo3.getString("subscribe");
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

    private JSONObject buildFeedRequest() {
        JSONObject jo = new JSONObject();
        jo.put("name", "JunitFeed");
        jo.put("version", "" + System.currentTimeMillis());  // make version unique
        jo.put("description", "Sample feed used by JUnit to test");

        JSONObject jo2 = new JSONObject();
        jo2.put("classification", "unrestricted");

        JSONObject jo3 = new JSONObject();
        jo3.put("id", "id001");
        jo3.put("password", "re1kwelj");
        JSONObject jo4 = new JSONObject();
        jo4.put("id", "id002");
        jo4.put("password", "o9eqlmbd");

        JSONArray ja = new JSONArray();
        ja.put(jo3);
        ja.put(jo4);
        jo2.put("endpoint_ids", ja);

        ja = new JSONArray();
        ja.put("10.0.0.1");
        ja.put("192.168.0.1");
        ja.put("135.207.136.128/25");
        jo2.put("endpoint_addrs", ja);

        jo.put("authorization", jo2);
        return jo;
    }
}
/*
curl -v -X POST -H 'X-DMAAP-DR-ON-BEHALF-OF: tester' -H 'Content-type: application/vnd.dmaap-dr.feed' \
    --user publisher:tomcat \
    --data "$data" http://127.0.0.1:8080/prov/feed/
*/