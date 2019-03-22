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
package org.onap.dmaap.datarouter.provisioning.beans;

import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.dmaap.datarouter.provisioning.utils.DB;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.InvalidObjectException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.anyString;

@RunWith(PowerMockRunner.class)
public class FeedTest {
    private static EntityManagerFactory emf;
    private static EntityManager em;
    private Feed feed;
    private DB db;

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
    public void setUp() throws Exception {
        db = new DB();
        feed = new Feed("Feed1","v0.1", "First Feed for testing", "First Feed for testing");
        feed.setFeedid(1);
        feed.setGroupid(1);
        feed.setPublisher("pub");
        feed.setDeleted(false);
    }

    @Test
    public void Given_getFilteredFeedUrlList_With_Name_Then_Method_Returns_Self_Links() {
        List<String>  list= feed.getFilteredFeedUrlList("name","Feed1");
        Assert.assertEquals("self_link",list.get(0));
    }

    @Test
    public void Given_getFilteredFeedUrlList_With_Publ_Then_Method_Returns_Self_Links() {
        List<String>  list= feed.getFilteredFeedUrlList("publ","pub");
        Assert.assertEquals("self_link",list.get(0));
    }

    @Test
    public void Given_getFilteredFeedUrlList_With_Subs_Then_Method_Returns_Self_Links() {
        List<String>  list= feed.getFilteredFeedUrlList("subs","sub123");
        Assert.assertEquals("self_link",list.get(0));
    }

    @Test
    public void Given_doDelete_Succeeds_Then_doInsert_To_Put_Feed_Back_And_Bool_Is_True() throws SQLException, InvalidObjectException {
        Boolean bool = feed.doDelete(db.getConnection());
        Assert.assertEquals(true, bool);
        JSONObject jo = new JSONObject();
        jo.put("self","self_link");
        jo.put("publish","publish_link");
        jo.put("subscribe","subscribe_link");
        jo.put("log","log_link");
        feed.setLinks(new FeedLinks(jo));
        bool = feed.doInsert(db.getConnection());
        Assert.assertEquals(true, bool);
    }

    @Test
    public void Validate_ChaneOwnerShip_Returns_True() {
        Boolean bool = feed.changeOwnerShip();
        Assert.assertEquals(true, bool);
    }

    @Test
    public void Given_Feeds_Are_Equal_Then_Equals_Returns_True() {
        Feed feed2 = feed;
        Boolean bool = feed.equals(feed2);
        Assert.assertEquals(true, bool);
    }

    @Test
    public void Validate_getFeedByNameVersion_Returns_Valid_Feed() {
        feed = Feed.getFeedByNameVersion("Feed1","v0.1");
        Assert.assertEquals(feed.toString(), "FEED: feedid=1, name=Feed1, version=v0.1");
    }

    @Test
    public void Given_doDelete_Throws_SQLException_Then_Returns_False() throws SQLException {
        Connection spyConnection = CreateSpyForDbConnection();
        Mockito.doThrow(new SQLException()).when(spyConnection).prepareStatement(anyString());
        Assert.assertEquals(feed.doDelete(spyConnection), false);
    }

    @Test
    public void Given_doInsert_Throws_SQLException_Then_Returns_False() throws SQLException {
        Connection connection = db.getConnection();
        FeedAuthorization fa = new FeedAuthorization();
        Set setA = new HashSet();
        setA.add(new FeedEndpointID("1", "Name"));
        Set setB = new HashSet();
        setB.add("172.0.0.1");
        fa.setEndpoint_ids(setA);
        fa.setEndpoint_addrs(setB);
        feed.setAuthorization(fa);
        Assert.assertEquals(feed.doInsert(connection), false);

    }

    @Test
    public void Given_doUpdate_Throws_SQLException_Then_Returns_False() throws SQLException {
        Connection spyConnection = CreateSpyForDbConnection();
        Mockito.doThrow(new SQLException()).when(spyConnection).prepareStatement(anyString());
        Assert.assertEquals(feed.doUpdate(spyConnection), false);

    }

    @Test
    public void Validate_Set_Get_Methods_Return_Correct_Values(){
        feed.setName("testName");
        feed.setVersion("v1.0");
        feed.setGroupid(1);
        feed.setDescription("test feed");
        feed.setBusiness_description("test feed");
        feed.setSuspended(false);
        feed.setPublisher("publish");

        Assert.assertEquals(feed.getName(), "testName");
        Assert.assertEquals(feed.getVersion(), "v1.0");
        Assert.assertEquals(feed.getGroupid(), 1);
        Assert.assertEquals(feed.getDescription(), "test feed");
        Assert.assertEquals(feed.getBusiness_description(), "test feed");
        Assert.assertEquals(feed.isSuspended(), false);
        Assert.assertEquals(feed.getPublisher(), "publish");
    }

    @Test
    public void Given_IsFeedValid_Called_And_Feed_Exists_Returns_True(){
        Assert.assertEquals(feed.isFeedValid(1), true);
    }

    private Connection CreateSpyForDbConnection() throws SQLException {
        Connection conn = db.getConnection();
        return Mockito.spy(conn);
    }
}