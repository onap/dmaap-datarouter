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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.dmaap.datarouter.provisioning.beans.Feed;
import org.onap.dmaap.datarouter.provisioning.beans.FeedAuthorization;
import org.onap.dmaap.datarouter.provisioning.beans.Group;
import org.onap.dmaap.datarouter.provisioning.beans.Subscription;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"org.onap.dmaap.datarouter.provisioning.beans.Feed",
        "org.onap.dmaap.datarouter.provisioning.beans.Subscription",
        "org.onap.dmaap.datarouter.provisioning.beans.Group"})
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
        assertThat(baseServlet.getIdFromPath(request), is(123));
    }

    @Test
    public void Given_Request_Path_Info_Is_Not_Valid_Then_Minus_One_Is_Returned() {
        when(request.getPathInfo()).thenReturn("/abc");
        assertThat(baseServlet.getIdFromPath(request), is(-1));
        when(request.getPathInfo()).thenReturn("/");
        assertThat(baseServlet.getIdFromPath(request), is(-1));
    }

    @Test
    public void Given_Remote_Address_Is_Known_And_RequireCerts_Is_True() throws Exception {
        when(request.isSecure()).thenReturn(true);
        Set<String> authAddressesAndNetworks = new HashSet<String>();
        authAddressesAndNetworks.add(("127.0.0.1"));
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "authorizedAddressesAndNetworks", authAddressesAndNetworks, true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "requireCert", true, true);
        assertNull(baseServlet.isAuthorizedForProvisioning(request));
    }

    @Test
    public void Given_Request_Is_GetFeedOwner_And_Feed_Exists() throws Exception {
        PowerMockito.mockStatic(Feed.class);
        Feed feed = mock(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(feed);
        when(feed.getPublisher()).thenReturn("stub_publisher");
        assertThat(baseServlet.getFeedOwner("3"), is("stub_publisher"));
    }

    @Test
    public void Given_Request_Is_GetFeedOwner_And_Feed_Does_Not_Exist() throws Exception {
        PowerMockito.mockStatic(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(null);
        assertThat(baseServlet.getFeedOwner("3"), is(nullValue()));
    }

    @Test
    public void Given_Request_Is_GetFeedClassification_And_Feed_Exists() throws Exception {
        PowerMockito.mockStatic(Feed.class);
        Feed feed = mock(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(feed);
        FeedAuthorization fAuth = mock(FeedAuthorization.class);
        when(feed.getAuthorization()).thenReturn(fAuth);
        when(fAuth.getClassification()).thenReturn("stub_classification");
        assertThat(baseServlet.getFeedClassification("3"), is("stub_classification"));
    }

    @Test
    public void Given_Request_Is_GetFeedClassification_And_Feed_Does_Not_Exist() throws Exception {
        PowerMockito.mockStatic(Feed.class);
        PowerMockito.when(Feed.getFeedById(anyInt())).thenReturn(null);
        assertThat(baseServlet.getFeedClassification("3"), is(nullValue()));
    }

    @Test
    public void Given_Request_Is_GetSubscriptionOwner_And_Subscription_Exists() throws Exception {
        PowerMockito.mockStatic(Subscription.class);
        Subscription subscription = mock(Subscription.class);
        PowerMockito.when(Subscription.getSubscriptionById(anyInt())).thenReturn(subscription);
        when(subscription.getSubscriber()).thenReturn("stub_subscriber");
        assertThat(baseServlet.getSubscriptionOwner("3"), is("stub_subscriber"));
    }

    @Test
    public void Given_Request_Is_GetSubscriptionOwner_And_Subscription_Does_Not_Exist() throws Exception {
        PowerMockito.mockStatic(Subscription.class);
        PowerMockito.when(Subscription.getSubscriptionById(anyInt())).thenReturn(null);
        assertThat(baseServlet.getSubscriptionOwner("3"), is(nullValue()));
    }

    @Test
    public void Given_Request_Is_GetGroupByFeedGroupId_And_User_Is_A_Member_Of_Group() throws Exception {
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
    public void Given_Request_Is_GetGroupByFeedGroupId_And_User_Is_Not_A_Member_Of_Group() throws Exception {
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
    public void Given_Request_Is_GetGroupBySubGroupId_And_User_Is_A_Member_Of_Group() throws Exception {
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
    public void Given_Request_Is_GetGroupBySubGroupId_And_User_Is_Not_A_Member_Of_Group() throws Exception {
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
}
