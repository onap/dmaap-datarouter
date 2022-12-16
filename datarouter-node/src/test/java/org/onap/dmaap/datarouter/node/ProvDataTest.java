/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */
package org.onap.dmaap.datarouter.node;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.dmaap.datarouter.node.config.ProvData;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*"})
public class ProvDataTest {


    @Test
    public void Validate_Values_Are_Set_Correctly_Through_ProvData_Constuctor() throws Exception {
        String InternalProvData =
                "{" +
                        "\"ingress\":[{" +
                        "\"feedid\":1," +
                        "\"subnet\":\"\"," +
                        "\"user\":\"\"," +
                        "\"node\":\"node\"" +
                        "}]," +
                        "\"routing\":[{" +
                        "\"from\":\"172.10.10.10\"," +
                        "\"to\":\"172.10.10.12\"," +
                        "\"via\":\"172.10.10.11\"" +
                        "}]," +
                        "\"subscriptions\":[{" +
                        "\"subid\":1," +
                        "\"suspend\":false," +
                        "\"delivery\":{" +
                        "\"use100\":true," +
                        "\"password\":\"PASSWORD\"," +
                        "\"user\":\"LOGIN\"," +
                        "\"url\":\"http://172.18.0.2:7070\"" +
                        "}," +
                        "\"last_mod\":1553608460000," +
                        "\"subscriber\":\"PMMAPER\"," +
                        "\"feedid\":1," +
                        "\"decompress\":false," +
                        "\"groupid\":1," +
                        "\"metadataOnly\":false," +
                        "\"follow_redirect\":false," +
                        "\"links\":{" +
                        "\"feed\":\"https://dmaap-dr-prov/feed/1\"" +
                        ",\"log\":\"https://dmaap-dr-prov/sublog/1\"" +
                        ",\"self\":\"https://dmaap-dr-prov/subs/1\"" +
                        "}," +
                        "\"created_date\":1553608460000," +
                        "\"privilegedSubscriber\":false" +
                        "}]," +
                        "\"feeds\":[{" +
                        "\"suspend\":false," +
                        "\"groupid\":0," +
                        "\"description\":\"Default feed\"," +
                        "\"version\":\"m1.0\"," +
                        "\"authorization\":{" +
                        "\"endpoint_addrs\":[\"172.10.10.20\"]," +
                        "\"classification\":\"unclassified\"," +
                        "\"endpoint_ids\":[{" +
                        "\"password\":\"password\"," +
                        "\"id\":\"user\"" +
                        "}]" +
                        "}," +
                        "\"last_mod\":1553608454000," +
                        "\"deleted\":false," +
                        "\"feedid\":1," +
                        "\"name\":\"CSIT_Test2\"" +
                        ",\"business_description\":\"Default Feed\"" +
                        ",\"publisher\":\"dradmin\"" +
                        ",\"links\":{" +
                        "\"subscribe\":\"https://dmaap-dr-prov/subscribe/1\"," +
                        "\"log\":\"https://dmaap-dr-prov/feedlog/1\"," +
                        "\"publish\":\"https://dmaap-dr-prov/publish/1\"," +
                        "\"self\":\"https://dmaap-dr-prov/feed/1\"" +
                        "}," +
                        "\"created_date\":1553608454000" +
                        "}]," +
                        "\"groups\":[]," +
                        "\"parameters\":{" +
                        "\"NODES\":[\"dmaap-dr-node\"]," +
                        "\"PROV_DOMAIN\":\"\"" +
                        "}," +
                        "\"egress\":{" +
                        "\"1\":1" +
                        "}" +
                        "}";
        Reader r = new InputStreamReader(new ByteArrayInputStream(InternalProvData.getBytes(StandardCharsets.UTF_8)));
        ProvData pd = new ProvData(r);

        assertEquals(pd.getNodes().length, 1);
        assertEquals(pd.getNodes()[0].getCName(), "dmaap-dr-node.");

        assertEquals(pd.getFeedUsers().length, 1);
        assertEquals(pd.getFeedUsers()[0].getUser(), "user");
        assertEquals(pd.getFeedUsers()[0].getFeedId(), "1");
        assertEquals(pd.getFeeds().length, 1);
        assertEquals(pd.getFeeds()[0].getId(), "1");
        assertEquals(pd.getFeedSubnets().length, 1);
        assertEquals(pd.getFeedSubnets()[0].getFeedId(), "1");
        assertEquals(pd.getFeedSubnets()[0].getCidr(), "172.10.10.20");
        assertEquals(pd.getFeedSubnets()[0].getCidr(), "172.10.10.20");
        assertEquals(pd.getSubscriptions()[0].getFeedId(), "1");
        assertEquals(pd.getSubscriptions()[0].getSubId(), "1");
        assertEquals(pd.getSubscriptions()[0].getAuthUser(), "LOGIN");
        assertEquals(pd.getSubscriptions()[0].getURL(), "http://172.18.0.2:7070");
        assertEquals(pd.getForceEgress().length, 1);
        assertEquals(pd.getForceEgress()[0].getNode(), "1");
        assertEquals(pd.getForceEgress()[0].getSubId(), "1");
        assertEquals(pd.getForceIngress().length, 1);
        assertEquals(pd.getForceIngress()[0].getFeedId(), "1");
        assertNull(pd.getForceIngress()[0].getSubnet());
        assertNull(pd.getForceIngress()[0].getUser());
        assertEquals(pd.getHops().length, 1);
        assertEquals(pd.getHops()[0].getFrom(), "172.10.10.10");
        assertEquals(pd.getHops()[0].getTo(), "172.10.10.12");
        assertEquals(pd.getHops()[0].getVia(), "172.10.10.11");
        assertEquals(pd.getParams().length, 1);
        assertEquals(pd.getParams()[0].getName(), "PROV_DOMAIN");
    }
}
