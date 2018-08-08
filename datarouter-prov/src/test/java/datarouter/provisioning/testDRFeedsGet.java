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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;

public class testDRFeedsGet extends testBase {
    private JSONArray returnedlist;

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
    public void testNormal() {
        testCommon(HttpServletResponse.SC_OK);
        int expect = 0;
        JSONArray ja = db_state.getJSONArray("feeds");
        for (int i = 0; i < ja.length(); i++) {
            JSONObject jo = ja.getJSONObject(i);
            if (!jo.getBoolean("deleted"))
                expect++;
        }
        if (returnedlist.length() != expect)
            fail("bad length, got "+ returnedlist.length() + " expect " + expect);
    }
    @Test
    public void testNormalGoodName() {
        JSONArray ja = db_state.getJSONArray("feeds");
        JSONObject feed0 = ja.getJSONObject(0);
        String name = feed0.getString("name");
        String query = "?name=" + name;
        int expect = 0;
        for (int n = 0; n < ja.length(); n++) {
            JSONObject jo = ja.getJSONObject(n);
            if (!jo.getBoolean("deleted") && jo.getString("name").equals(name))
                expect++;
        }
        testCommon(HttpServletResponse.SC_OK, query, FeedServlet.FEEDLIST_CONTENT_TYPE, "JUnit");
        if (returnedlist.length() != expect)
            fail("bad length, got "+ returnedlist.length() + " expect "+expect);
    }
    @Test
    public void testNormalBadName() {
        String query = "?name=ZZTOP123456";
        testCommon(HttpServletResponse.SC_OK, query, FeedServlet.FEEDLIST_CONTENT_TYPE, "JUnit");
        if (returnedlist.length() != 0)
            fail("bad length, got "+ returnedlist.length() + " expect 0");
    }
    @Test
    public void testNormalBadPath() {
        String query = "flarg/?publisher=JUnit";
        testCommon(HttpServletResponse.SC_NOT_FOUND, query, "text/html;charset=ISO-8859-1", "JUnit");
    }
    @Test
    public void testNormalGoodPublisher() {
        JSONArray ja = db_state.getJSONArray("feeds");
        JSONObject feed0 = ja.getJSONObject(0);
        String query = "?publisher=" + feed0.getString("publisher");
        testCommon(HttpServletResponse.SC_OK, query, FeedServlet.FEEDLIST_CONTENT_TYPE, "JUnit");
        int expect = 0;
        for (int i = 0; i < ja.length(); i++) {
            JSONObject jo = ja.getJSONObject(i);
            if (jo.getString("publisher").equals(feed0.getString("publisher")) && !jo.getBoolean("deleted"))
                expect++;
        }
        if (returnedlist.length() != expect)
            fail("bad length, got "+returnedlist.length()+" expected "+expect);
    }
    @Test
    public void testNormalBadPublisher() {
        String query = "?publisher=ZZTOP123456";
        testCommon(HttpServletResponse.SC_OK, query, FeedServlet.FEEDLIST_CONTENT_TYPE, "JUnit");
        if (returnedlist.length() != 0)
            fail("bad length");
    }
    @Test
    public void testNormalGoodSubscriber() {
        JSONArray ja = db_state.getJSONArray("subscriptions");
        if (ja.length() > 0) {
            JSONObject sub0 = ja.getJSONObject(0);
            String query = "?subscriber=" + sub0.getString("subscriber");
            testCommon(HttpServletResponse.SC_OK, query, FeedServlet.FEEDLIST_CONTENT_TYPE, "JUnit");
// aarg! - this is complicated!
//        int expect = 0;
//        for (int i = 0; i < ja.length(); i++) {
//            JSONObject jo = ja.getJSONObject(i);
//            if (jo.getString("subscriber").equals(sub0.getString("subscriber")))
//                expect++;
//        }
//        if (returnedlist.length() != 1)
//            fail("bad length "+returnedlist.toString());
        } else {
            // There are no subscriptions yet, so use a made up name
            testCommon(HttpServletResponse.SC_OK, "?subscriber=foo", FeedServlet.FEEDLIST_CONTENT_TYPE, "JUnit");
        }
    }
    @Test
    public void testNormalBadSubscriber() {
        String query = "?subscriber=ZZTOP123456";
        testCommon(HttpServletResponse.SC_OK, query, FeedServlet.FEEDLIST_CONTENT_TYPE, "JUnit");
        if (returnedlist.length() != 0)
            fail("bad length");
    }
    private void testCommon(int expect) {
        testCommon(expect, "", FeedServlet.FEEDLIST_CONTENT_TYPE, "JUnit");
    }
    private void testCommon(int expect, String query, String ectype, String bhdr) {
        String url = props.getProperty("test.host") + "/" + query;
        HttpGet httpGet = new HttpGet(url);
        try {
            if (bhdr != null)
                httpGet.addHeader(FeedServlet.BEHALF_HEADER, bhdr);

            HttpResponse response = httpclient.execute(httpGet);
            ckResponse(response, expect);

            HttpEntity entity = response.getEntity();
            String ctype = entity.getContentType().getValue().trim();
            if (!ctype.equals(ectype))
                fail("Got wrong content type: "+ctype);

            // do something useful with the response body and ensure it is fully consumed
            if (ctype.equals(FeedServlet.FEEDLIST_CONTENT_TYPE)) {
                try {
                    returnedlist = new JSONArray(new JSONTokener(entity.getContent()));
                } catch (Exception e) {
                    fail("Bad JSON: "+e.getMessage());
                }
            } else {
                EntityUtils.consume(entity);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            httpGet.releaseConnection();
        }
    }
}
