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

package org.onap.dmaap.datarouter.provisioning;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.att.eelf.configuration.EELFManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.onap.dmaap.datarouter.provisioning.utils.LogfileLoader;
import org.onap.dmaap.datarouter.provisioning.utils.RLEBitSet;
import org.onap.dmaap.datarouter.provisioning.utils.URLUtilities;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({BaseServlet.class, URLUtilities.class, EELFManager.class})
public class SynchronizerTaskTest {

    @Mock
    private AbstractHttpClient httpClient;

    @Mock
    private HttpEntity httpEntity;

    @Mock
    private StatusLine statusLine;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private ByteArrayOutputStream byteArrayOutputStream;

    private SynchronizerTask synchronizerTask;

    private ExecutorService executorService;

    private static EntityManagerFactory emf;
    private static EntityManager em;

    @BeforeClass
    public static void init() {
        emf = Persistence.createEntityManagerFactory("dr-unit-tests");
        em = emf.createEntityManager();
        System.setProperty(
                "org.onap.dmaap.datarouter.provserver.properties",
                "src/test/resources/h2Database.properties");
    }

    @AfterClass
    public static void tearDownClass() {
        em.clear();
        em.close();
        emf.close();
    }


    @Before
    public void setUp() throws IllegalAccessException, UnknownHostException {
        SSLSocketFactory sslSocketFactory = mock(SSLSocketFactory.class);
        doNothing().when(sslSocketFactory).setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        PowerMockito.mockStatic(BaseServlet.class);
        PowerMockito.mockStatic(URLUtilities.class);
        when(BaseServlet.getPods()).thenReturn(new String[] {InetAddress.getLocalHost().getHostName(), "stand-by-prov"});
        when(URLUtilities.generatePeerProvURL()).thenReturn("https://stand-by-prov/internal/prov");
        when(URLUtilities.generatePeerLogsURL()).thenReturn("https://stand-by-prov/internal/drlogs");

        synchronizerTask = Mockito.spy(SynchronizerTask.getSynchronizer());
        doReturn(2).when(synchronizerTask).lookupState();

        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(synchronizerTask);
    }

    @After
    public void tearDown() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test
    public void Given_Synch_Task_readRemoteLoglist_Called_And_Valid_BitSet_Returned_Success() throws Exception {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(200);
        Mockito.when(httpEntity.getContentType()).thenReturn(new BasicHeader("header", "text/plain"));
        Mockito.when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("1-55251".getBytes()));
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        Assert.assertNotNull(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_readRemoteLoglist_Called_And_Invalid_Resonse_Code_Failure() throws Exception {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(404);
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        Assert.assertNotNull(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_readRemoteLoglist_Called_And_Invalid_Content_Type_Failure() throws Exception {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(200);
        Mockito.when(httpEntity.getContentType()).thenReturn(new BasicHeader("header", "invalid_content_type"));
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        Assert.assertNotNull(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_replicateDataRouterLogs_Called_And_Valid_BitSet_Returned_Success() throws Exception {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(200);
        Mockito.when(httpEntity.getContentType()).thenReturn(new BasicHeader("header", "text/plain"));
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        synchronizerTask.replicateDataRouterLogs(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_replicateDataRouterLogs_Called_And_Invalid_Content_Type_Failure() throws Exception {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(200);
        Mockito.when(httpEntity.getContentType()).thenReturn(new BasicHeader("header", "invalid_content_type"));
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        synchronizerTask.replicateDataRouterLogs(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_replicateDataRouterLogs_Called_And_Invalid_Resonse_Code_Failure() throws Exception {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(404);
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        synchronizerTask.replicateDataRouterLogs(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_Is_Started_And_LogFileLoader_Is_Idle_Then_Standby_Pod_Synch_Is_Successful() throws Exception {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(200);
        Mockito.when(httpEntity.getContentType()).thenReturn(new BasicHeader("header", "application/vnd.dmaap-dr.provfeed-full; version=1.0"));
        mockResponseFromGet();
    }


    private void mockHttpClientForGetRequest() throws Exception {
        FieldUtils.writeField(synchronizerTask, "httpclient", httpClient, true);
        Mockito.when(httpClient.execute(anyObject())).thenReturn(response);
        Mockito.when(response.getEntity()).thenReturn(httpEntity);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);

    }

    private void mockResponseFromGet() throws IOException {
        InputStream in = setUpProvData();
        Mockito.when(httpEntity.getContent()).thenReturn(in);
    }

    private InputStream setUpProvData() {
        String provDataString = "{\n" + "  \"feeds\": [\n" + "    {\n" + "      \"suspend\": false,\n"
                                        + "      \"groupid\": 0,\n"
                                        + "      \"description\": \"Default feed provisioned for PM File collector\",\n"
                                        + "      \"version\": \"m1.0\",\n" + "      \"authorization\": {\n"
                                        + "        \"endpoint_addrs\": [\n" + "          \n" + "        ],\n"
                                        + "        \"classification\": \"unclassified\",\n"
                                        + "        \"endpoint_ids\": [\n" + "          {\n"
                                        + "            \"password\": \"dradmin\",\n"
                                        + "            \"id\": \"dradmin\"\n" + "          }\n" + "        ]\n"
                                        + "      },\n" + "      \"last_mod\": 1560871903000,\n"
                                        + "      \"deleted\": false,\n" + "      \"feedid\": 1,\n"
                                        + "      \"name\": \"Default PM Feed\",\n"
                                        + "      \"business_description\": \"Default Feed\",\n"
                                        + "      \"aaf_instance\": \"legacy\",\n"
                                        + "      \"publisher\": \"dradmin\",\n" + "      \"links\": {\n"
                                        + "        \"subscribe\": \"https://dmaap-dr-prov/subscribe/1\",\n"
                                        + "        \"log\": \"https://dmaap-dr-prov/feedlog/1\",\n"
                                        + "        \"publish\": \"https://dmaap-dr-prov/publish/1\",\n"
                                        + "        \"self\": \"https://dmaap-dr-prov/feed/1\"\n" + "      },\n"
                                        + "      \"created_date\": 1560871903000\n" + "    }\n" + "  ],\n"
                                        + "  \"groups\": [\n" + "    {\n"
                                        + "      \"authid\": \"GROUP-0000-c2754bb7-92ef-4869-9c6b-1bc1283be4c0\",\n"
                                        + "      \"name\": \"Test Group\",\n"
                                        + "      \"description\": \"Test Description of Group .\",\n"
                                        + "      \"classification\": \"publisher/subscriber\",\n"
                                        + "      \"members\": \"{id=attuid, name=User1}, {id=attuid, name=User 2]\"\n"
                                        + "    }\n" + "  ],\n" + "  \"subscriptions\": [\n" + "    {\n"
                                        + "      \"suspend\": false,\n" + "      \"delivery\": {\n"
                                        + "        \"use100\": true,\n" + "        \"password\": \"PASSWORD\",\n"
                                        + "        \"user\": \"LOGIN\",\n"
                                        + "        \"url\": \"https://dcae-pm-mapper:8443/delivery\"\n" + "      },\n"
                                        + "      \"subscriber\": \"dradmin\",\n" + "      \"groupid\": 0,\n"
                                        + "      \"metadataOnly\": false,\n" + "      \"privilegedSubscriber\": true,\n"
                                        + "      \"subid\": 1,\n" + "      \"last_mod\": 1560872889000,\n"
                                        + "      \"feedid\": 1,\n" + "      \"follow_redirect\": false,\n"
                                        + "      \"decompress\": true,\n" + "      \"aaf_instance\": \"legacy\",\n"
                                        + "      \"links\": {\n"
                                        + "        \"feed\": \"https://dmaap-dr-prov/feed/1\",\n"
                                        + "        \"log\": \"https://dmaap-dr-prov/sublog/1\",\n"
                                        + "        \"self\": \"https://dmaap-dr-prov/subs/1\"\n" + "      },\n"
                                        + "      \"created_date\": 1560872889000\n" + "    }\n" + "  ],\n"
                                        + "  \"parameters\": {\n" + "    \"ACTIVE_POD\": \"dmaap-dr-prov\",\n"
                                        + "    \"DELIVERY_FILE_PROCESS_INTERVAL\": 10,\n"
                                        + "    \"DELIVERY_INIT_RETRY_INTERVAL\": 10,\n"
                                        + "    \"DELIVERY_MAX_AGE\": 86400,\n"
                                        + "    \"DELIVERY_MAX_RETRY_INTERVAL\": 3600,\n"
                                        + "    \"DELIVERY_RETRY_RATIO\": 2,\n" + "    \"LOGROLL_INTERVAL\": 30,\n"
                                        + "    \"NODES\": [\n" + "      \"dmaap-dr-node\"\n" + "    ],\n"
                                        + "    \"PROV_ACTIVE_NAME\": \"dmaap-dr-prov\",\n"
                                        + "    \"PROV_AUTH_ADDRESSES\": [\n" + "      \"dmaap-dr-prov\",\n"
                                        + "      \"dmaap-dr-node\"\n" + "    ],\n" + "    \"PROV_AUTH_SUBJECTS\": [\n"
                                        + "      \"\"\n" + "    ],\n" + "    \"PROV_DOMAIN\": \"\",\n"
                                        + "    \"PROV_MAXFEED_COUNT\": 10000,\n"
                                        + "    \"PROV_MAXSUB_COUNT\": 100000,\n"
                                        + "    \"PROV_NAME\": \"dmaap-dr-prov\",\n"
                                        + "    \"PROV_REQUIRE_CERT\": \"false\",\n"
                                        + "    \"PROV_REQUIRE_SECURE\": \"true\",\n" + "    \"STANDBY_POD\": \"\",\n"
                                        + "    \"_INT_VALUES\": [\n" + "      \"LOGROLL_INTERVAL\",\n"
                                        + "      \"PROV_MAXFEED_COUNT\",\n" + "      \"PROV_MAXSUB_COUNT\",\n"
                                        + "      \"DELIVERY_INIT_RETRY_INTERVAL\",\n"
                                        + "      \"DELIVERY_MAX_RETRY_INTERVAL\",\n"
                                        + "      \"DELIVERY_RETRY_RATIO\",\n" + "      \"DELIVERY_MAX_AGE\",\n"
                                        + "      \"DELIVERY_FILE_PROCESS_INTERVAL\"\n" + "    ]\n" + "  },\n"
                                        + "  \"ingress\": [\n" + "    {\n" + "      \"feedid\": 1,\n"
                                        + "      \"subnet\": \"\",\n" + "      \"user\": \"\",\n"
                                        + "      \"node\": [\n" + "        \"stub_from.\"\n" + "      ]\n" + "    }\n"
                                        + "  ],\n" + "  \"egress\": {\n" + "    \"1\": \"stub_to.\"\n" + "  },\n"
                                        + "  \"routing\": [\n" + "    {\n" + "      \"from\": 1,\n"
                                        + "      \"to\": 3,\n" + "      \"via\": 2\n" + "    }\n" + "  ]\n" + "}" ;


        return new ByteArrayInputStream(provDataString.getBytes());
    }

}
