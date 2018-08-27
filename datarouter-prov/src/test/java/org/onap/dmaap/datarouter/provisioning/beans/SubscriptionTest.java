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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"org.onap.dmaap.datarouter.provisioning.beans.Subscription"})
public class SubscriptionTest {

    private Subscription subscription;

    @Test
    public void validate_Subscription_Created_With_Default_Constructor() {
        subscription = new Subscription();
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

        subscription = new Subscription();
        subscription.setGroupid(2);
        subscription.setDelivery(subDelivery);
        subscription.setMetadataOnly(false);
        subscription.setSubscriber(subscriber);
        subscription.setSuspended(false);
        subscription.setLinks(subLinks);

        Assert.assertEquals(2, subscription.getGroupid());
        Assert.assertEquals(subDelivery, subscription.getDelivery());
        Assert.assertEquals(subLinks, subscription.getLinks());
        Assert.assertFalse(subscription.isMetadataOnly());
        Assert.assertFalse(subscription.isSuspended());
    }
}