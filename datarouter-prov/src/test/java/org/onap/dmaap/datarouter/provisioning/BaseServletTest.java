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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKeyFactory;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.provisioning.beans.Feed;
import org.onap.dmaap.datarouter.provisioning.beans.FeedAuthorization;
import org.onap.dmaap.datarouter.provisioning.beans.Group;
import org.onap.dmaap.datarouter.provisioning.beans.Subscription;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.MDC;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"org.onap.dmaap.datarouter.provisioning.beans.Feed",
        "org.onap.dmaap.datarouter.provisioning.beans.Subscription",
        "org.onap.dmaap.datarouter.provisioning.beans.Group"})
@PowerMockIgnore({"javax.crypto.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*"})
@PrepareForTest({UUID.class, SecretKeyFactory.class})
public class BaseServletTest extends DrServletTestBase {

    private BaseServlet baseServlet;

    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        baseServlet = new BaseServlet();
    }


    @Test
    public void Given_Request_Path_Info_Is_Valid_Then_Id_Is_Extracted_Correctly() {
        when(request.getPathInfo()).thenReturn("/123");
        assertThat(BaseServlet.getIdFromPath(request), is(123));
    }

    @Test
    public void Given_Request_Path_Info_Is_Not_Valid_Then_Minus_One_Is_Returned() {
        when(request.getPathInfo()).thenReturn("/abc");
        assertThat(BaseServlet.getIdFromPath(request), is(-1));
        when(request.getPathInfo()).thenReturn("/");
        assertThat(BaseServlet.getIdFromPath(request), is(-1));
    }

    @Test
    public void Given_Remote_Address_Is_Known_And_RequireCerts_Is_True() throws Exception {
        when(request.isSecure()).thenReturn(true);
        Set<String> authAddressesAndNetworks = new HashSet<>();
        authAddressesAndNetworks.add(("127.0.0.1"));
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "authorizedAddressesAndNetworks", authAddressesAndNetworks, true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "requireCert", true, true);
        assertNull(baseServlet.isAuthorizedForProvisioning(request));
    }

    @Test
    public void Given_Request_Is_GetFeedOwner_And_Feed_Exists() {
        PowerMockito.mockStatic(Feed.class);
        Feed feed = mock(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(feed);
        when(feed.getPublisher()).thenReturn("stub_publisher");
        assertThat(baseServlet.getFeedOwner("3"), is("stub_publisher"));
    }

    @Test
    public void Given_Request_Is_GetFeedOwner_And_Feed_Does_Not_Exist(){
        PowerMockito.mockStatic(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(null);
        assertThat(baseServlet.getFeedOwner("3"), is(nullValue()));
    }

    @Test
    public void Given_Request_Is_GetFeedClassification_And_Feed_Exists(){
        PowerMockito.mockStatic(Feed.class);
        Feed feed = mock(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(feed);
        FeedAuthorization fAuth = mock(FeedAuthorization.class);
        when(feed.getAuthorization()).thenReturn(fAuth);
        when(fAuth.getClassification()).thenReturn("stub_classification");
        assertThat(baseServlet.getFeedClassification("3"), is("stub_classification"));
    }

    @Test
    public void Given_Request_Is_GetFeedClassification_And_Feed_Does_Not_Exist() {
        PowerMockito.mockStatic(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(null);
        assertThat(baseServlet.getFeedClassification("3"), is(nullValue()));
    }

    @Test
    public void Given_Request_Is_GetSubscriptionOwner_And_Subscription_Exists() {
        PowerMockito.mockStatic(Subscription.class);
        Subscription subscription = mock(Subscription.class);
        PowerMockito.when(Subscription.getSubscriptionById(anyInt())).thenReturn(subscription);
        when(subscription.getSubscriber()).thenReturn("stub_subscriber");
        assertThat(baseServlet.getSubscriptionOwner("3"), is("stub_subscriber"));
    }

    @Test
    public void Given_Request_Is_GetSubscriptionOwner_And_Subscription_Does_Not_Exist() {
        PowerMockito.mockStatic(Subscription.class);
        PowerMockito.when(Subscription.getSubscriptionById(anyInt())).thenReturn(null);
        assertThat(baseServlet.getSubscriptionOwner("3"), is(nullValue()));
    }

    @Test
    public void Given_Request_Is_GetGroupByFeedGroupId_And_User_Is_A_Member_Of_Group() {
        PowerMockito.mockStatic(Feed.class);
        Feed feed = mock(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(feed);
        when(feed.getGroupid()).thenReturn(3);
        PowerMockito.mockStatic(Group.class);
        Group group = mock(Group.class);
        when(group.getMembers()).thenReturn("{id: stub_user}");
        PowerMockito.when(Group.getGroupById(anyInt())).thenReturn(group);
        when(group.getAuthid()).thenReturn("stub_authID");
        assertThat(baseServlet.getGroupByFeedGroupId("stub_user", "3"), is("stub_authID"));
    }

    @Test
    public void Given_Request_Is_GetGroupByFeedGroupId_And_User_Is_Not_A_Member_Of_Group() {
        PowerMockito.mockStatic(Feed.class);
        Feed feed = mock(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(feed);
        when(feed.getGroupid()).thenReturn(3);
        PowerMockito.mockStatic(Group.class);
        Group group = mock(Group.class);
        when(group.getMembers()).thenReturn("{id: stub_otherUser}");
        PowerMockito.when(Group.getGroupById(anyInt())).thenReturn(group);
        when(group.getAuthid()).thenReturn("stub_authID");
        assertThat(baseServlet.getGroupByFeedGroupId("stub_user", "3"), is(nullValue()));
    }

    @Test
    public void Given_Request_Is_GetGroupBySubGroupId_And_User_Is_A_Member_Of_Group() {
        PowerMockito.mockStatic(Subscription.class);
        Subscription subscription = mock(Subscription.class);
        PowerMockito.when(Subscription.getSubscriptionById(anyInt())).thenReturn(subscription);
        when(subscription.getGroupid()).thenReturn(3);
        PowerMockito.mockStatic(Group.class);
        Group group = mock(Group.class);
        when(group.getMembers()).thenReturn("{id: stub_user}");
        PowerMockito.when(Group.getGroupById(anyInt())).thenReturn(group);
        when(group.getAuthid()).thenReturn("stub_authID");
        assertThat(baseServlet.getGroupBySubGroupId("stub_user", "3"), is("stub_authID"));
    }

    @Test
    public void Given_Request_Is_GetGroupBySubGroupId_And_User_Is_Not_A_Member_Of_Group() {
        PowerMockito.mockStatic(Subscription.class);
        Subscription subscription = mock(Subscription.class);
        PowerMockito.when(Subscription.getSubscriptionById(anyInt())).thenReturn(subscription);
        when(subscription.getGroupid()).thenReturn(3);
        PowerMockito.mockStatic(Group.class);
        Group group = mock(Group.class);
        when(group.getMembers()).thenReturn("{id: stub_otherUser}");
        PowerMockito.when(Group.getGroupById(anyInt())).thenReturn(group);
        when(group.getAuthid()).thenReturn("stub_authID");
        assertThat(baseServlet.getGroupBySubGroupId("stub_user", "3"), is(nullValue()));
    }

    @Test
    public void Given_Request_Has_Empty_RequestId_And_InvocationId_Headers_Generate_MDC_Values() {
        when(request.getHeader("X-ONAP-RequestID")).thenReturn("");
        when(request.getHeader("X-InvocationID")).thenReturn("");
        mockStatic(UUID.class);
        when(UUID.randomUUID().toString()).thenReturn("123", "456");
        baseServlet.setIpFqdnRequestIDandInvocationIDForEelf("doDelete", request);
        Assert.assertNotEquals("123", MDC.get("RequestId"));
        Assert.assertNotEquals("456", MDC.get("InvocationId"));
    }

    @Test
    public void Given_Request_Has_RequestId_And_InvocationId_Headers_Set_MDC_Values() {
        when(request.getHeader("X-ONAP-RequestID")).thenReturn("123");
        when(request.getHeader("X-InvocationID")).thenReturn("456");
        baseServlet.setIpFqdnRequestIDandInvocationIDForEelf("doDelete", request);
        Assert.assertEquals("123", MDC.get("RequestId"));
        Assert.assertEquals("456", MDC.get("InvocationId"));
    }

    public JSONObject getFeedJsonObject() {
        return new JSONObject("{\"authorization\": {\n" + "    \"endpoint_addrs\": [\n" + "    ],\n"
                                      + "    \"classification\": \"unclassified\",\n"
                                      + "    \"endpoint_ids\": [\n" + "      {\n"
                                      + "        \"password\": \"dradmin\",\n"
                                      + "        \"id\": \"dradmin\"\n" + "      },\n" + "      {\n"
                                      + "        \"password\": \"demo123456!\",\n"
                                      + "        \"id\": \"onap\"\n" + "      }\n" + "    ]\n" + "  }}");
    }

    public JSONObject getSubJsonObject() {
        return new JSONObject("{\"delivery\": {\"url\": \"http://172.18.0.3:7070/\", \"user\": "
            + "\"LOGIN\", \"password\": \"PASSWORD\", \"use100\": true}, \"metadataOnly\": false, "
            + "\"suspend\": false, \"groupid\": 29, \"subscriber\": \"sg481n\"}");
    }

    @Test
    public void Given_Debug_Is_Enabled_Hash_Feed_Passwords_Successful() {
        JSONObject hashed_feed_pass = BaseServlet.hashPasswords(getFeedJsonObject());
        assertNotEquals(hashed_feed_pass.getJSONObject("authorization").getJSONArray("endpoint_ids")
            .getJSONObject(0).get("password").toString(), "demo123456!");

    }

    @Test
    public void Given_Debug_Is_Enabled_Hash_Sub_Passwords_Successful() {
        JSONObject hashed_sub_pass = BaseServlet.hashPasswords(getSubJsonObject());
        assertNotEquals(hashed_sub_pass.getJSONObject("delivery").get("password").toString(), "PASSWORD");

    }

    @Test
    public void Given_BaseServlet_Verify_Cadi_Feed_Permission() {
        assertEquals("org.onap.dmaap-dr.feed|legacy|publish", baseServlet.getFeedPermission("legacy", "publish"));
        assertEquals("org.onap.dmaap-dr.feed|legacy|suspend", baseServlet.getFeedPermission("legacy", "suspend"));
        assertEquals("org.onap.dmaap-dr.feed|legacy|restore", baseServlet.getFeedPermission("legacy", "restore"));
        assertEquals("org.onap.dmaap-dr.feed|org.onap.dmaap-dr.NoInstanceDefined|restore", baseServlet.getFeedPermission(null, "restore"));
        assertEquals("org.onap.dmaap-dr.feed|legacy|*", baseServlet.getFeedPermission("legacy", "default"));
    }

    @Test
    public void Given_BaseServlet_Verify_Cadi_Sub_Permission() {
        assertEquals("org.onap.dmaap-dr.feed|legacy|subscribe", baseServlet.getSubscriberPermission("legacy", "subscribe"));
        assertEquals("org.onap.dmaap-dr.sub|legacy|suspend", baseServlet.getSubscriberPermission("legacy", "suspend"));
        assertEquals("org.onap.dmaap-dr.sub|legacy|restore", baseServlet.getSubscriberPermission("legacy", "restore"));
        assertEquals("org.onap.dmaap-dr.sub|legacy|publish", baseServlet.getSubscriberPermission("legacy", "publish"));
        assertEquals("org.onap.dmaap-dr.sub|org.onap.dmaap-dr.NoInstanceDefined|restore", baseServlet.getSubscriberPermission(null, "restore"));
        assertEquals("org.onap.dmaap-dr.sub|legacy|*", baseServlet.getSubscriberPermission("legacy", "default"));
    }

}
