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
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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
    public void Given_Request_Path_Info_Is_Not_Valid_Then_Minus_One_Is() throws Exception {
        when(request.isSecure()).thenReturn(true);
        Set<String> authAddressesAndNetworks = new HashSet<String>();
        authAddressesAndNetworks.add(("127.0.0.1"));
        FieldUtils
            .writeDeclaredStaticField(BaseServlet.class, "authorizedAddressesAndNetworks", authAddressesAndNetworks,
                true);
        FieldUtils.writeDeclaredStaticField(BaseServlet.class, "requireCert", false, true);
        assertThat(baseServlet.isAuthorizedForProvisioning(request), is(nullValue()));
    }
}
