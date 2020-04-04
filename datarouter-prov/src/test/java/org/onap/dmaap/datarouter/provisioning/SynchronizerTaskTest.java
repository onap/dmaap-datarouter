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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import org.onap.dmaap.datarouter.provisioning.utils.RLEBitSet;
import org.onap.dmaap.datarouter.provisioning.utils.SynchronizerTask;
import org.onap.dmaap.datarouter.provisioning.utils.URLUtilities;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
@PrepareForTest({BaseServlet.class, URLUtilities.class})
public class SynchronizerTaskTest {

    @Mock
    private AbstractHttpClient httpClient;

    @Mock
    private HttpEntity httpEntity;

    @Mock
    private StatusLine statusLine;

    @Mock
    private CloseableHttpResponse response;

    private SynchronizerTask synchronizerTask;

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
    }

    @After
    public void tearDown() {
    }

    @Test
    public void Given_Synch_Task_readRemoteLoglist_Called_And_Valid_BitSet_Returned_Success()
            throws IOException, IllegalAccessException {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(200);
        Mockito.when(httpEntity.getContentType()).thenReturn(new BasicHeader("header", "text/plain"));
        Mockito.when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("1-55251".getBytes()));
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        Assert.assertNotNull(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_readRemoteLoglist_Called_And_Invalid_Resonse_Code_Failure()
            throws IOException, IllegalAccessException {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(404);
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        Assert.assertNotNull(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_readRemoteLoglist_Called_And_Invalid_Content_Type_Failure()
            throws IOException, IllegalAccessException {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(200);
        Mockito.when(httpEntity.getContentType()).thenReturn(new BasicHeader("header", "invalid_content_type"));
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        Assert.assertNotNull(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_replicateDataRouterLogs_Called_And_Valid_BitSet_Returned_Success()
            throws IOException, IllegalAccessException {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(200);
        Mockito.when(httpEntity.getContentType()).thenReturn(new BasicHeader("header", "text/plain"));
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        synchronizerTask.replicateDataRouterLogs(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_replicateDataRouterLogs_Called_And_Invalid_Content_Type_Failure()
            throws IOException, IllegalAccessException {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(200);
        Mockito.when(httpEntity.getContentType()).thenReturn(new BasicHeader("header", "invalid_content_type"));
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        synchronizerTask.replicateDataRouterLogs(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_replicateDataRouterLogs_Called_And_Invalid_Resonse_Code_Failure()
            throws IOException, IllegalAccessException {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(404);
        RLEBitSet rleBitSet = synchronizerTask.readRemoteLoglist();
        synchronizerTask.replicateDataRouterLogs(rleBitSet);
    }

    @Test
    public void Given_Synch_Task_Is_Started_And_LogFileLoader_Is_Idle_Then_Standby_Pod_Synch_Is_Successful()
            throws IOException, IllegalAccessException {
        mockHttpClientForGetRequest();
        Mockito.when(response.getStatusLine().getStatusCode()).thenReturn(200);
        Mockito.when(httpEntity.getContentType()).thenReturn(new BasicHeader("header", "application/vnd.dmaap-dr.provfeed-full; version=1.0"));
        mockResponseFromGet();
        synchronizerTask.run();
    }


    private void mockHttpClientForGetRequest() throws IllegalAccessException, IOException {
        FieldUtils.writeField(synchronizerTask, "httpclient", httpClient, true);
        Mockito.when(httpClient.execute(anyObject())).thenReturn(response);
        Mockito.when(response.getEntity()).thenReturn(httpEntity);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);

    }

    private void mockResponseFromGet() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("prov_data.json");
        Mockito.when(httpEntity.getContent()).thenReturn(in);
    }
}
