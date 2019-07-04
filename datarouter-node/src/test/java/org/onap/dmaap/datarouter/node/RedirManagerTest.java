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

import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Timer;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class RedirManagerTest {

    private RedirManager redirManager;
    private String redirFilePath = System.getProperty("user.dir") + "/src/test/resources/redir_file";

    @Before
    public void setUp() {
        Timer timer = new Timer("Node Configuration Timer", true);
        redirManager = new RedirManager(redirFilePath, 10000L, timer);
    }

    @Test
    public void Given_Lookup_On_Valid_Redirect_Returns_Target_URL() {
        assertThat(redirManager.lookup("1", "http://destination:8443/path/to"), Is.is("http://redirect:8443/path/to"));
    }

    @Test
    public void Given_IsRedirected_Called_On_Valid_Sub_Id_Then_Returns_True() {
        assertThat(redirManager.isRedirected("1"), Is.is(true));
    }

    @Test
    public void Given_Redirect_Called_On_Valid_Redirect_New_Redirect_Added() throws IOException {
        long origFileLenght = new File(redirFilePath).length();
        redirManager.redirect("3", "http://destination3:8443/path/to", "http://redirect3:8443/path/to");
        assertThat(redirManager.lookup("3", "http://destination3:8443/path/to"), Is.is("http://redirect3:8443/path/to"));
        new RandomAccessFile(redirFilePath, "rw").setLength(origFileLenght);
    }

    @Test
    public void Given_Lookup_On_Invalid_Redirect_Returns_Primary_Target_URL_And_Is_Forgotten() throws IOException {
        assertThat(redirManager.lookup("2", "http://invalid:8443/path/to"), Is.is("http://invalid:8443/path/to"));
        Files.write(Paths.get(redirFilePath), "2 http://destination2:8443/path/to http://redirect2:8443/path/to".getBytes(), StandardOpenOption.APPEND);
    }
}
