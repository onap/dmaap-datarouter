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


package org.onap.dmaap.datarouter.node;

/**
 * A destination to deliver a message
 */
public class Target {
    private DestInfo destinfo;
    private String routing;

    /**
     * A destination to deliver a message
     *
     * @param destinfo Either info for a subscription ID or info for a node-to-node transfer
     * @param routing  For a node-to-node transfer, what to do when it gets there.
     */
    public Target(DestInfo destinfo, String routing) {
        this.destinfo = destinfo;
        this.routing = routing;
    }

    /**
     * Add additional routing
     */
    public void addRouting(String routing) {
        this.routing = this.routing + " " + routing;
    }

    /**
     * Get the destination information for this target
     */
    public DestInfo getDestInfo() {
        return (destinfo);
    }

    /**
     * Get the next hop information for this target
     */
    public String getRouting() {
        return (routing);
    }
}
