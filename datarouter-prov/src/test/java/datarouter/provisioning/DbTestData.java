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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.FeedServlet;

import java.io.IOException;
import java.util.Properties;

/**
 * The DbTestData class
 *
 * @version 1.0.1
 */
public class DbTestData {

    private static boolean dbReady = false;

    public static void populateDb(AbstractHttpClient httpclient, Properties props) {
        if (!dbReady) {
            JSONObject jo = buildFeedRequest();
            for (int i = 0; i < 10; i++) {
                jo.put("version", "" + System.currentTimeMillis());
                int statusCode = -1;
                String url = props.getProperty("test.host");
                HttpPost httpPost = new HttpPost(url);
                try {
                    httpPost.addHeader(FeedServlet.BEHALF_HEADER, "JUnit");
                    String feedRequestString = jo.toString();
                    HttpEntity body = new ByteArrayEntity(feedRequestString.getBytes(),
                        ContentType.create(FeedServlet.FEED_CONTENT_TYPE));
                    httpPost.setEntity(body);
                    HttpResponse response = httpclient.execute(httpPost);
                    statusCode = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();
                    EntityUtils.consume(entity);
                } catch (IOException e) {
                    System.err.println(e);
                } finally {
                    httpPost.releaseConnection();
                }
                System.out.println(i + " " + statusCode);
            }
            dbReady = true;
        }
    }

    private static JSONObject buildFeedRequest() {
        JSONObject jo = new JSONObject();
        jo.put("name", "feed");
        jo.put("version", "" + System.currentTimeMillis());
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
