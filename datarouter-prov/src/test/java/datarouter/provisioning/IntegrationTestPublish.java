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
import org.apache.http.HttpResponse;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;

public class IntegrationTestPublish extends IntegrationTestBase {
    private String publishUrl;

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
        // Get publish URL from first feed
        JSONArray ja = db_state.getJSONArray("feeds");
        for (int i = ja.length() - 1; i >= 0; i--) {
            JSONObject feed = ja.getJSONObject(i);
            if (!feed.getBoolean("deleted")) {
                publishUrl = feed.getJSONObject("links").getString("publish");
                publishUrl += "/" + System.currentTimeMillis();
                return;
            }
        }
    }

    @Test
    public void testDelete() {
        HttpDelete httpDelete = new HttpDelete(publishUrl);
        testCommon(httpDelete);
    }

    @Test
    public void testGet() {
        HttpGet httpGet = new HttpGet(publishUrl);
        testCommon(httpGet);
    }

    @Test
    public void testPut() {
        HttpPut httpPut = new HttpPut(publishUrl);
        testCommon(httpPut);
    }

    @Test
    public void testPost() {
        HttpPost httpPost = new HttpPost(publishUrl);
        testCommon(httpPost);
    }

    private void testCommon(HttpRequestBase rb) {
        try {
            rb.addHeader(FeedServlet.BEHALF_HEADER, "JUnit");
            RedirectStrategy strategy = new DefaultRedirectStrategy() {
                protected boolean isRedirectable(String method) {
                    return false;
                }
            };
            httpclient.setRedirectStrategy(strategy);
            HttpResponse response = httpclient.execute(rb);
            ckResponse(response, HttpServletResponse.SC_MOVED_PERMANENTLY);

            // Make sure there is a Location hdr
            Header[] loc = response.getHeaders("Location");
            if (loc == null || loc.length == 0) {
                fail("No location header");
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            rb.releaseConnection();
        }
    }
}
