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
package org.onap.dmaap.datarouter.provisioning.utils;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.AbstractHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DRRouteCLITest {

    @Mock
    private AbstractHttpClient httpClient;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private HttpEntity httpEntity;

    @Mock
    private StatusLine statusLine;

    private DRRouteCLI drRouteCLI;

    @Before
    public void setUp() throws IllegalAccessException {
        drRouteCLI = mock(DRRouteCLI.class);
        doCallRealMethod().when(drRouteCLI).runCommand(anyObject());
        FieldUtils.writeField(drRouteCLI, "server", "prov.datarouternew.com", true);
    }

    @Test
    public void Given_Add_Egress_Then_RunCommand_Returns_True() throws Exception {
        mockHttpClientForRestCall();
        Assert.assertTrue(drRouteCLI.runCommand(new String[]{"add", "egress", "1", "node.datarouternew.com"}));
    }

    @Test
    public void Given_Add_Network_Then_RunCommand_Returns_True() throws Exception {
        mockHttpClientForRestCall();
        Assert.assertTrue(drRouteCLI.runCommand(
            new String[]{"add", "network", "prov.datarouternew.com", "node.datarouternew.com", "172.100.0.1"}));
    }

    @Test
    public void Given_Add_Egress_With_Incorrect_Args_Then_RunCommand_Returns_False() throws Exception {
        mockHttpClientForRestCall();
        Assert.assertFalse(drRouteCLI
            .runCommand(new String[]{"add", "egress", "1", "user1", "172.100.0.0", "node.datarouternew.com"}));
    }

    @Test
    public void Given_Error_On_Post_Rest_Call_RunCommand_Returns_False() throws Exception {
        mockErrorResponseFromRestCall();
        Assert.assertFalse(
            drRouteCLI.runCommand(new String[]{"add", "network", "prov.datarouternew.com", "node.datarouternew.com"}));
    }

    @Test
    public void Given_Delete_Ingress_Then_RunCommand_Returns_True() throws Exception {
        mockHttpClientForRestCall();
        Assert.assertTrue(drRouteCLI.runCommand(new String[]{"del", "ingress", "1", "user1", "172.100.0.0"}));
    }

    @Test
    public void Given_Delete_Egress_Then_RunCommand_Returns_True() throws Exception {
        mockHttpClientForRestCall();
        Assert.assertTrue(drRouteCLI.runCommand(new String[]{"del", "egress", "1"}));
    }

    @Test
    public void Given_Delete_Network_Then_RunCommand_Returns_True() throws Exception {
        mockHttpClientForRestCall();
        Assert.assertTrue(
            drRouteCLI.runCommand(new String[]{"del", "network", "prov.datarouternew.com", "node.datarouternew.com"}));
    }

    @Test
    public void Given_Delete_Ingress_With_Incorrect_Args_Then_RunCommand_Returns_False() throws Exception {
        mockHttpClientForRestCall();
        Assert.assertFalse(
            drRouteCLI.runCommand(new String[]{"del", "ingress", "prov.datarouternew.com", "node.datarouternew.com"}));
    }

    @Test
    public void Given_Error_On_Delete_Rest_Call_RunCommand_Returns_False() throws Exception {
        mockErrorResponseFromRestCall();
        Assert.assertFalse(
            drRouteCLI.runCommand(new String[]{"del", "network", "prov.datarouternew.com", "node.datarouternew.com"}));
    }

    @Test
    public void Given_List_Args_Then_RunCommand_Returns_True() throws Exception {
        mockHttpClientForGetRequest();
        Assert.assertTrue(drRouteCLI.runCommand(new String[]{"list"}));
    }

    @Test
    public void Given_Error_On_Get_Rest_Call_RunCommand_Returns_True() throws Exception {
        mockErrorResponseFromRestCall();
        Assert.assertTrue(drRouteCLI.runCommand(new String[]{"list"}));
    }

    @Test
    public void Given_Width_Arg_Then_RunCommand_Returns_True() {
        Assert.assertTrue(drRouteCLI.runCommand(new String[]{"width", "130"}));
    }

    @Test
    public void Given_Usage_Arg_Then_RunCommand_Returns_False() {
        Assert.assertFalse(drRouteCLI.runCommand(new String[]{"usage"}));
    }

    private void mockHttpClientForRestCall() throws Exception {
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(anyObject())).thenReturn(httpResponse);
        FieldUtils.writeField(drRouteCLI, "httpclient", httpClient, true);
    }

    private void mockHttpClientForGetRequest() throws Exception {
        mockResponseFromGet();
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(anyObject())).thenReturn(httpResponse);
        FieldUtils.writeField(drRouteCLI, "httpclient", httpClient, true);
    }

    private void mockResponseFromGet() throws IOException {
        JSONObject response = new JSONObject();
        response.put("ingress", addIngressObject());
        response.put("egress", addEgressObject());
        response.put("routing", addRoutingObject());
        InputStream in = new ByteArrayInputStream(response.toString().getBytes());
        when(httpEntity.getContent()).thenReturn(in);
    }

    private JSONArray addRoutingObject() {
        JSONArray routing = new JSONArray();
        JSONObject route = new JSONObject();
        route.put("from", "prov.datarouternew.com");
        route.put("to", "node.datarouternew.com");
        route.put("via", "172.100.0.1");
        routing.put(route);
        return routing;
    }

    private JSONObject addEgressObject() {
        JSONObject egress = new JSONObject();
        egress.put("1", "node.datarouternew.com");
        egress.put("2", "172.0.0.1");
        return egress;
    }

    private JSONArray addIngressObject() {
        JSONArray ingresses = new JSONArray();
        JSONObject ingress = new JSONObject();
        ingress.put("seq", 21);
        ingress.put("feedid", 1);
        ingress.put("user", "user1");
        ingress.put("subnet", "172.0.0.0");
        JSONArray nodes = new JSONArray();
        nodes.put("node.datarouternew.com");
        nodes.put("172.0.0.1");
        ingress.put("node", nodes);
        ingresses.put(ingress);
        return ingresses;
    }

    private void mockErrorResponseFromRestCall() throws Exception {
        InputStream in = new ByteArrayInputStream("<pre> Server Not Found </pre>".getBytes());
        when(httpEntity.getContent()).thenReturn(in);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(statusLine.getStatusCode()).thenReturn(400);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(anyObject())).thenReturn(httpResponse);
        FieldUtils.writeField(drRouteCLI, "httpclient", httpClient, true);
    }
}
