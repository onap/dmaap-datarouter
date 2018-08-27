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