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
package org.onap.dmaap.datarouter.node;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"org.onap.dmaap.datarouter.node.ProvData", "org.onap.dmaap.datarouter.node.NodeUtils"})
public class NodeConfigTest {

    private static NodeConfig nodeConfig;

    @BeforeClass
    public static void setUp() throws IOException{
        ProvData provData = setUpProvData();
        nodeConfig = new NodeConfig(provData, "Name", "spool/dir", 80, "Key");
    }

    @Test
    public void Given_Feed_Does_Not_Exist_Then_Is_Publish_Permitted_Returns_Not_Null() {
        String permitted = nodeConfig.isPublishPermitted("2", "user", "0.0.0.0");
        Assert.assertEquals("Feed does not exist", permitted);
    }

    @Test
    public void Given_Feed_But_User_Not_Permitted_Then_Is_Publish_Permitted_Returns_Not_Null() {
        String permitted = nodeConfig.isPublishPermitted("1", "user", "0.0.0.0");
        Assert.assertEquals("Publisher not permitted for this feed", permitted);
    }

    @Test
    public void Given_Feed_But_Ip_Does_Not_Match_Then_Is_Publish_Permitted_Returns_Not_Null() {
        String permitted = nodeConfig.isPublishPermitted("1", "Basic dXNlcjE6cGFzc3dvcmQx", "0.0.0.0");
        Assert.assertEquals("Publisher not permitted for this feed", permitted);
    }

    @Test
    public void Given_Feed_Then_Is_Publish_Permitted_Returns_Null() {
        String permitted = nodeConfig.isPublishPermitted("1", "Basic dXNlcjE6cGFzc3dvcmQx", "172.0.0.1");
        Assert.assertNull(permitted);
    }

    @Test
    public void Given_SubId_Then_Get_Feed_Id_Returns_Correct_Id() {
        String feedId = nodeConfig.getFeedId("1");
        Assert.assertEquals("1", feedId);
    }

    @Test
    public void Given_Incorrect_SubId_Then_Get_Feed_Id_Returns_Null() {
        String feedId = nodeConfig.getFeedId("2");
        Assert.assertNull(feedId);
    }

    @Test
    public void Given_SubId_Then_Get_Spool_Dir_Returns_Correct_Id() {
        String spoolDir = nodeConfig.getSpoolDir("1");
        Assert.assertEquals("spool/dir/s/0/1", spoolDir);
    }

    @Test
    public void Given_Incorrect_SubId_Then_Get_Spool_Dir_Returns_Null() {
        String spoolDir = nodeConfig.getSpoolDir("2");
        Assert.assertNull(spoolDir);
    }

    @Test
    public void Given_Feed_And_Incorrect_Credentials_Then_Get_Auth_User_Returns_Null() {
        String authUser = nodeConfig.getAuthUser("1", "incorrect");
        Assert.assertNull(authUser);
    }

    @Test
    public void Given_Feed_And_Correct_Credentials_Then_Get_Auth_User_Returns_User() {
        String authUser = nodeConfig.getAuthUser("1", "Basic dXNlcjE6cGFzc3dvcmQx");
        Assert.assertEquals("user1", authUser);
    }

    @Test
    public void Given_Correct_Feed_Then_Get_Ingress_Node_Returns_Node() {
        String node = nodeConfig.getIngressNode("1", "user1", "172.0.0.1");
        Assert.assertEquals("172.0.0.4", node);
    }

    @Test
    public void Given_Correct_Feed_Then_Get_Targets_Returns_Correct_Dest_Info() {
        Target[] targets = nodeConfig.getTargets("1");
        Assert.assertEquals("1", targets[0].getDestInfo().getSubId());
        Assert.assertEquals("spool/dir/s/0/1", targets[0].getDestInfo().getSpool());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void Given_Null_Feed_Then_Get_Targets_Returns_Empty_Array() {
        Target[] targets = nodeConfig.getTargets(null);
        targets[0].getDestInfo();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void Given_Incorrect_Feed_Then_Get_Targets_Returns_Empty_Array() {
        Target[] targets = nodeConfig.getTargets("2");
        targets[0].getDestInfo();
    }

    @Test
    public void Given_Same_Ip_Then_Is_Another_Node_Returns_False() {
        Boolean isAnotherNode = nodeConfig.isAnotherNode("Basic MTcyLjAuMC40OmtCTmhkWVFvbzhXNUphZ2g4T1N4Zmp6Mzl1ND0=", "172.0.0.1");
        Assert.assertFalse(isAnotherNode);
    }

    @Test
    public void Given_Different_Ip_Then_Is_Another_Node_Returns_True() {
        Boolean isAnotherNode = nodeConfig.isAnotherNode("Basic MTcyLjAuMC40OmtCTmhkWVFvbzhXNUphZ2g4T1N4Zmp6Mzl1ND0=", "172.0.0.4");
        Assert.assertTrue(isAnotherNode);
    }

    @Test
    public void Given_Param_Name_Then_Get_Prov_Param_Returns_Parameter() {
        String paramValue = nodeConfig.getProvParam("DELIVERY_MAX_AGE");
        Assert.assertEquals("86400", paramValue);
    }

    @Test
    public void Validate_Get_All_Dests_Returns_Dest_Info() {
        DestInfo[] destInfo = nodeConfig.getAllDests();
        Assert.assertEquals("n:172.0.0.4", destInfo[0].getName());
    }

    @Test
    public void Validate_Get_MyAuth_Returns_Correct_Auth() {
        String auth = nodeConfig.getMyAuth();
        Assert.assertEquals("Basic TmFtZTp6Z04wMFkyS3gybFppbXltNy94ZDhuMkdEYjA9", auth);
    }

    private static ProvData setUpProvData() throws IOException {
        JSONObject provData = new JSONObject();
        createValidFeed(provData);
        createValidSubscription(provData);
        createValidParameters(provData);
        createValidIngressValues(provData);
        createValidEgressValues(provData);
        createValidRoutingValues(provData);
        Reader reader = new StringReader(provData.toString());
        return new ProvData(reader);
    }

    private static void createValidFeed(JSONObject provData) {
        JSONArray feeds = new JSONArray();
        JSONObject feed = new JSONObject();
        JSONObject auth = new JSONObject();
        JSONArray endpointIds = new JSONArray();
        JSONArray endpointAddrs = new JSONArray();
        JSONObject endpointId = new JSONObject();
        feed.put("feedid", "1");
        feed.put("name", "Feed1");
        feed.put("version", "m1.0");
        feed.put("suspend", false);
        feed.put("deleted", false);
        endpointId.put("id", "user1");
        endpointId.put("password", "password1");
        endpointIds.put(endpointId);
        auth.put("endpoint_ids", endpointIds);
        endpointAddrs.put("172.0.0.1");
        auth.put("endpoint_addrs", endpointAddrs);
        feed.put("authorization", auth);
        feeds.put(feed);
        provData.put("feeds", feeds);
    }

    private static void createValidSubscription(JSONObject provData) {
        JSONArray subscriptions = new JSONArray();
        JSONObject subscription = new JSONObject();
        JSONObject delivery = new JSONObject();
        subscription.put("subid", "1");
        subscription.put("feedid", "1");
        subscription.put("suspend", false);
        subscription.put("metadataOnly", false);
        delivery.put("url", "https://172.0.0.2");
        delivery.put("user", "user1");
        delivery.put("password", "password1");
        delivery.put("use100", true);
        subscription.put("delivery", delivery);
        subscriptions.put(subscription);
        provData.put("subscriptions", subscriptions);
    }

    private static void createValidParameters(JSONObject provData) {
        JSONObject parameters = new JSONObject();
        JSONArray nodes = new JSONArray();
        parameters.put("PROV_NAME", "prov.datarouternew.com");
        parameters.put("DELIVERY_INIT_RETRY_INTERVAL", "10");
        parameters.put("DELIVERY_MAX_AGE", "86400");
        parameters.put("PROV_DOMAIN", "");
        nodes.put("172.0.0.4");
        parameters.put("NODES", nodes);
        provData.put("parameters", parameters);
    }

    private static void createValidIngressValues(JSONObject provData) {
        JSONArray ingresses = new JSONArray();
        JSONObject ingress = new JSONObject();
        ingress.put("feedid", "1");
        ingress.put("subnet", "");
        ingress.put("user", "");
        ingress.put("node", "172.0.0.4");
        ingresses.put(ingress);
        provData.put("ingress", ingresses);
    }

    private static void createValidEgressValues(JSONObject provData) {
        JSONObject egress = new JSONObject();
        egress.put("subid", "1");
        egress.put("nodeid", "172.0.0.4");
        provData.put("egress", egress);
    }

    private static void createValidRoutingValues(JSONObject provData) {
        JSONArray routings = new JSONArray();
        JSONObject routing = new JSONObject();
        routing.put("from", "prov.datarouternew.com");
        routing.put("to", "172.0.0.4");
        routing.put("via", "172.100.0.1");
        routings.put(routing);
        provData.put("routing", routings);
    }
}
