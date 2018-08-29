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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.onap.dmaap.datarouter.provisioning.beans.Feed")
public class IngressRouteTest {

  private IngressRoute ingressRoute;

  @Before
  public void setUp() throws IllegalAccessException{
    PowerMockito.mockStatic(Feed.class);
    Feed feed = mock(Feed.class);
    PowerMockito.when(Feed.getFeedById(1)).thenReturn(feed);
    Map<String, Integer> map = new HashMap<>();
    FieldUtils.writeDeclaredStaticField(NodeClass.class, "map", map, true);
  }

  @Test
  public void Validate_IngressRoute_Constructors_Create_Same_Object() {
    List<String> nodes = new ArrayList<>();
    nodes.add("node.datarouternew.com");
    ingressRoute = new IngressRoute(1, 1, "user1", "172.100.0.0/25", nodes);
    JSONObject ingressRouteJson = createIngressRouteJson();
    Assert.assertEquals(ingressRoute, new IngressRoute(ingressRouteJson));
  }

  @Test
  public void Validate_AsJsonObject_Returns_Same_Values() {
    List<String> nodes = new ArrayList<>();
    nodes.add("node.datarouternew.com");
    ingressRoute = new IngressRoute(1, 1, "user1", "172.100.0.0/25", nodes);
    JSONObject ingressRouteJson = createIngressRouteJson();
    Assert.assertEquals(ingressRoute.asJSONObject().toString(), ingressRouteJson.toString());
  }

  private JSONObject createIngressRouteJson() {
    JSONObject ingressRouteJson = new JSONObject();
    ingressRouteJson.put("seq", 1);
    ingressRouteJson.put("feedid", 1);
    ingressRouteJson.put("user", "user1");
    ingressRouteJson.put("subnet", "172.100.0.0/25");
    JSONArray nodes = new JSONArray();
    nodes.put("node.datarouternew.com");
    ingressRouteJson.put("node", nodes);
    return ingressRouteJson;
  }

}
