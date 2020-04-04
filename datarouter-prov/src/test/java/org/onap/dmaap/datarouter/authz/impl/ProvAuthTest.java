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

package org.onap.dmaap.datarouter.authz.impl;

import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.http.HttpServletRequest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.onap.dmaap.datarouter.authz.AuthorizationResponse;
import org.onap.dmaap.datarouter.provisioning.StatisticsServlet;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*",
    "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*" })
public class ProvAuthTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private StatisticsServlet statisticsServlet;

    private ProvAuthorizer provAuthorizer;

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
        provAuthorizer = new ProvAuthorizer(statisticsServlet);
    }

    @Test
    public void Validate_Prov_Auth_Check_Feed_Access() {
        when(statisticsServlet.getFeedOwner(Mockito.anyString())).thenReturn("dr-admin");
        when(statisticsServlet.getGroupByFeedGroupId(Mockito.anyString(), Mockito.anyString())).thenReturn("stub_auth_id");
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF")).thenReturn("dr-admin");
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_auth_id");
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("http://the-request-uri:443/feed/1?1");
        AuthorizationResponse authResp;
        authResp = provAuthorizer.decide(request);
        Assert.assertTrue(authResp.isAuthorized());
    }

    @Test
    public void Validate_Prov_Auth_Check_Sub_Access() {
        when(statisticsServlet.getSubscriptionOwner(Mockito.anyString())).thenReturn("dr-admin");
        when(statisticsServlet.getGroupBySubGroupId(Mockito.anyString(), Mockito.anyString())).thenReturn("stub_auth_id");
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF")).thenReturn("dr-admin");
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_auth_id");
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("http://the-request-uri:443/subs/1?1");
        AuthorizationResponse authResp;
        authResp = provAuthorizer.decide(request);
        Assert.assertTrue(authResp.isAuthorized());
    }

    @Test
    public void Validate_Prov_Auth_Check_Subs_Collection_Access() {
        when(statisticsServlet.getSubscriptionOwner(Mockito.anyString())).thenReturn("dr-admin");
        when(statisticsServlet.getGroupBySubGroupId(Mockito.anyString(), Mockito.anyString())).thenReturn("stub_auth_id");
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF")).thenReturn("dr-admin");
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_auth_id");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("http://the-request-uri:443/subscribe/1?1");
        AuthorizationResponse authResp;
        authResp = provAuthorizer.decide(request);
        Assert.assertTrue(authResp.isAuthorized());
    }

    @Test
    public void Validate_Prov_Auth_Check_Feeds_Collection_Access() {
        when(statisticsServlet.getFeedOwner(Mockito.anyString())).thenReturn("dr-admin");
        when(statisticsServlet.getGroupByFeedGroupId(Mockito.anyString(), Mockito.anyString())).thenReturn("stub_auth_id");
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF")).thenReturn("dr-admin");
        when(request.getHeader("X-DMAAP-DR-ON-BEHALF-OF-GROUP")).thenReturn("stub_auth_id");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("http://the-request-uri:443/");
        AuthorizationResponse authResp;
        authResp = provAuthorizer.decide(request);
        Assert.assertTrue(authResp.isAuthorized());
        Assert.assertNull(authResp.getAdvice());
        Assert.assertNull(authResp.getObligations());
    }

}
