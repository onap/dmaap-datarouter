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
import org.json.JSONTokener;
import org.junit.Test;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;

public class IntegrationTestInternalMisc extends IntegrationTestBase {
    @Test
    public void testInternalDrlogs() {
        String url   = props.getProperty("test.host") + "/internal/drlogs";
        HttpGet httpPost = new HttpGet(url);
        try {
            httpPost.addHeader(FeedServlet.BEHALF_HEADER, "JUnit");
            HttpResponse response = httpclient.execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                fail("Unexpected response, expect " + HttpServletResponse.SC_NOT_FOUND + " got " + code);
            }

            HttpEntity entity = response.getEntity();
            String ctype = entity.getContentType().getValue().trim();
            boolean ok  = ctype.equals("text/plain");
            if (!ok) {
                fail("Got wrong content type: " + ctype);
            }

            EntityUtils.consume(entity);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            httpPost.releaseConnection();
        }
    }

    @Test
    public void testInternalHalt() {
        String url   = props.getProperty("test.host") + "/internal/halt";
        HttpGet httpPost = new HttpGet(url);
        try {
            httpPost.addHeader(FeedServlet.BEHALF_HEADER, "JUnit");

            HttpResponse response = httpclient.execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            if (code != HttpServletResponse.SC_NOT_FOUND) {
                fail("Unexpected response, expect " + HttpServletResponse.SC_NOT_FOUND + " got " + code);
            }

            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            httpPost.releaseConnection();
        }
    }

    @SuppressWarnings("unused")
    @Test
    public void testInternalLogs() {
        String url   = props.getProperty("test.host") + "/internal/logs";
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
            boolean ok  = ctype.equals("application/json");
            if (!ok) {
                fail("Got wrong content type: " + ctype);
            }

            // do something useful with the response body and ensure it is fully consumed
            if (ok) {
                try {
                    new JSONArray(new JSONTokener(entity.getContent()));
                } catch (Exception e) {
                    fail("Bad JSON: " + e.getMessage());
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

    @Test
    public void testInternalBadUrl() {
        String url   = props.getProperty("test.host") + "/internal/badurl";
        HttpGet httpPost = new HttpGet(url);
        try {
            httpPost.addHeader(FeedServlet.BEHALF_HEADER, "JUnit");

            HttpResponse response = httpclient.execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            if (code != HttpServletResponse.SC_NOT_FOUND) {
                fail("Unexpected response, expect " + HttpServletResponse.SC_NOT_FOUND + " got " + code);
            }

            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            httpPost.releaseConnection();
        }
    }

}
