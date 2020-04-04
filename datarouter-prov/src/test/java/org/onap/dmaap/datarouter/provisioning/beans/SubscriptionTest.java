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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class SubscriptionTest {

    private Subscription subscription;

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
    public void setUp() throws Exception {
        subscription = new Subscription();
    }

    @Test
    public void validate_Subscription_Created_With_Default_Constructor() {
        Assert.assertEquals(subscription.getSubid(), -1);
        Assert.assertEquals(subscription.getGroupid(), -1);
        Assert.assertEquals(subscription.getSubscriber(), "");
    }

    @Test
    public void validate_Getters_And_Setters() {
        String url = "1.2.3.4";
        String user = "myUser";
        String password = "myPass";
        String subscriber = "mySubscriber";
        SubDelivery subDelivery = new SubDelivery(url, user, password, false);
        SubLinks subLinks = new SubLinks();
        subLinks.setFeed("feed");
        subLinks.setLog("log");
        subLinks.setSelf("self");

        subscription.setGroupid(2);
        subscription.setDelivery(subDelivery);
        subscription.setMetadataOnly(false);
        subscription.setSubscriber(subscriber);
        subscription.setSuspended(false);
        subscription.setPrivilegedSubscriber(false);
        subscription.setFollowRedirect(true);
        subscription.setLinks(subLinks);
        subscription.setDecompress(false);

        Assert.assertEquals(2, subscription.getGroupid());
        Assert.assertEquals(subDelivery, subscription.getDelivery());
        Assert.assertEquals(subLinks, subscription.getLinks());
        Assert.assertFalse(subscription.isMetadataOnly());
        Assert.assertFalse(subscription.isSuspended());
        Assert.assertFalse(subscription.isPrivilegedSubscriber());
        Assert.assertFalse(subscription.isDecompress());

        Subscription sub2 = new Subscription();
        sub2.setGroupid(2);
        sub2.setDelivery(subDelivery);
        sub2.setMetadataOnly(false);
        sub2.setSubscriber(subscriber);
        sub2.setSuspended(false);
        sub2.setPrivilegedSubscriber(false);
        sub2.setFollowRedirect(true);
        sub2.setLinks(subLinks);
        sub2.setDecompress(false);
        Assert.assertTrue(subscription.equals(sub2));
        Assert.assertNotNull(sub2.toString());
        sub2.hashCode();
    }
}