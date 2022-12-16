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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.dmaap.datarouter.node.config.NodeConfig;
import org.onap.dmaap.datarouter.node.config.PathFinder;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class PathFinderTest {

    @Test
    public void Given_Unknown_From_Node_Returns_Null() {
        new PathFinder("dr-node-1", new String[]{"dr-node-1", "dr-node-2", "dr-node-3"},
                new NodeConfig.ProvHop[]{new NodeConfig.ProvHop("dr-node-4", "dr-node-3", "dr-node-2")});
    }

    @Test
    public void Given_Unknown_Destination_Node_Returns_Null() {
        new PathFinder("dr-node-1", new String[]{"dr-node-1", "dr-node-2", "dr-node-3"},
                new NodeConfig.ProvHop[]{new NodeConfig.ProvHop("dr-node-1", "dr-node-5", "dr-node-2")});
    }

    @Test
    public void Given_Duplicate_Next_Hop_Returns_Null() {
        PathFinder p = new PathFinder("dr-node-1", new String[]{"dr-node-1", "dr-node-2", "dr-node-3"},
                new NodeConfig.ProvHop[]{new NodeConfig.ProvHop("dr-node-1", "dr-node-3", "dr-node-2"),
                        new NodeConfig.ProvHop("dr-node-1", "dr-node-3", "dr-node-2")});
        assertThat(p.getErrors().length, is(1));
        assertNotNull(p.getPath("dr-node-3"));
        assertThat(p.getPath("dr-node-5").length(), is(0));
    }

    @Test
    public void Given_Unknown_Via_Node_Returns_Null() {
        new PathFinder("dr-node-1", new String[]{"dr-node-1", "dr-node-2", "dr-node-3"},
                new NodeConfig.ProvHop[]{new NodeConfig.ProvHop("dr-node-1", "dr-node-3", "dr-node-4")});
    }

    @Test
    public void Given_Dest_Equals_Via_Bad_Hop_Defined() {
        new PathFinder("dr-node-1", new String[]{"dr-node-1", "dr-node-2", "dr-node-3"},
                new NodeConfig.ProvHop[]{new NodeConfig.ProvHop("dr-node-1", "dr-node-2", "dr-node-2")});
    }

    @Test
    public void Given_Valid_Path_Defined_Success() {
        new PathFinder("dr-node-1", new String[]{"dr-node-1", "dr-node-2", "dr-node-3"},
                new NodeConfig.ProvHop[]{new NodeConfig.ProvHop("dr-node-1", "dr-node-3+", "dr-node-2")});
    }


}
