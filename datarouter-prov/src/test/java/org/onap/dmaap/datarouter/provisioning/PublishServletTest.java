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

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Created by ezcoxem on 21/08/2018.
 */

@RunWith(PowerMockRunner.class)
public class PublishServletTest {

    private static EntityManagerFactory emf;
    private static EntityManager em;
    private PublishServlet publishServlet;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

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
        publishServlet = new PublishServlet();
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_There_Are_No_Nodes_Then_Service_Unavailable_Error_Is_Returned()
        throws Exception {
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "nodes", new String[0], true);
        publishServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE), argThat(notNullValue(String.class)));
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "nodes", new String[1], true);
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Path_Is_Null_Then_Not_Found_Error_Is_Returned() throws Exception {
        publishServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Ix_Is_Null_Then_Not_Found_Error_Is_Returned() throws Exception {
        when(request.getPathInfo()).thenReturn("/1/");
        publishServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Feed_Is_Not_Valid_Then_Not_Found_Error_Is_Returned() throws Exception {
        when(request.getPathInfo()).thenReturn("/122/fileName.txt");
        publishServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }

    @Test
    public void Given_Request_Is_HTTP_DELETE_And_Feed_Is_Not_A_Number_Then_Not_Found_Error_Is_Returned()
        throws Exception {
        when(request.getPathInfo()).thenReturn("/abc/fileName.txt");
        publishServlet.doDelete(request, response);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), argThat(notNullValue(String.class)));
    }


    @Test
    public void Given_Request_Is_HTTP_DELETE_And_All_Ok_Then_Request_succeeds() throws Exception {
        when(request.getHeader(anyString())).thenReturn("Basic dXNlcg==");
        setConditionsForPositiveSuccessFlow();
        publishServlet.doDelete(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_MOVED_PERMANENTLY));
    }

    @Test
    public void Given_Request_Is_HTTP_PUT_And_Request_succeeds() throws Exception {
        setConditionsForPositiveSuccessFlow();
        publishServlet.doPut(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_MOVED_PERMANENTLY));
    }

    @Test
    public void Given_Request_Is_HTTP_POST_And_Request_succeeds() throws Exception {
        setConditionsForPositiveSuccessFlow();
        publishServlet.doPost(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_MOVED_PERMANENTLY));
    }

    @Test
    public void Given_Request_Is_HTTP_GET_And_Request_succeeds() throws Exception {
        setConditionsForPositiveSuccessFlow();
        publishServlet.doGet(request, response);
        verify(response).setStatus(eq(HttpServletResponse.SC_MOVED_PERMANENTLY));
    }

    private void setConditionsForPositiveSuccessFlow() throws Exception {
        FieldUtils.writeDeclaredField(publishServlet, "provstring", "", true);
        when(request.getPathInfo()).thenReturn("/1/fileName.txt");
    }
}
