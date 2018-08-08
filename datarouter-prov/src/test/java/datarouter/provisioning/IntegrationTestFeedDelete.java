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
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;

public class IntegrationTestFeedDelete extends IntegrationTestBase {
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
    public void testDeleteNormal() {
        // Delete the first non-deleted feed in the DB
        JSONArray ja = db_state.getJSONArray("feeds");
        for (int i = ja.length() - 1; i >= 0; i--) {
            JSONObject feed = ja.getJSONObject(i);
            if (!feed.getBoolean("deleted")) {
                int feedid = feed.getInt("feedid");
                testCommon(HttpServletResponse.SC_NO_CONTENT, "/feed/" + feedid);
                return;
            }
        }
    }

    @Test
    public void testDeleteNoFeedId() {
        testCommon(HttpServletResponse.SC_BAD_REQUEST, "/feed/");
    }

    @Test
    public void testDeleteNoFeed() {
        testCommon(HttpServletResponse.SC_NOT_FOUND, "/feed/999999");
    }

    private void testCommon(int expect, String uri) {
        String url = props.getProperty("test.host") + uri;
        HttpDelete del = new HttpDelete(url);
        try {
            del.addHeader(FeedServlet.BEHALF_HEADER, "JUnit");

            HttpResponse response = httpclient.execute(del);
            ckResponse(response, expect);

            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            del.releaseConnection();
        }
    }
}
