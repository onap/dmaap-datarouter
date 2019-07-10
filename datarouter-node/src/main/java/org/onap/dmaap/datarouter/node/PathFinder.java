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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.jetbrains.annotations.Nullable;
import org.onap.dmaap.datarouter.node.NodeConfig.ProvHop;

/**
 * Given a set of node names and next hops, identify and ignore any cycles and figure out the sequence of next hops to
 * get from this node to any other node.
 */

class PathFinder {

    private ArrayList<String> errors = new ArrayList<>();
    private HashMap<String, String> routes = new HashMap<>();

    /**
     * Find routes from a specified origin to all of the nodes given a set of specified next hops.
     *
     * @param origin where we start
     * @param nodes where we can go
     * @param hops detours along the way
     */
    PathFinder(String origin, String[] nodes, NodeConfig.ProvHop[] hops) {
        HashSet<String> known = new HashSet<>();
        HashMap<String, HashMap<String, Hop>> ht = new HashMap<>();
        for (String n : nodes) {
            known.add(n);
            ht.put(n, new HashMap<>());
        }
        for (NodeConfig.ProvHop ph : hops) {
            Hop hop = getHop(known, ht, ph);
            if (hop == null) {
                continue;
            }
            if (ph.getVia().equals(ph.getTo())) {
                errors.add(ph + " gives destination as via");
                hop.bad = true;
            }
        }
        for (String n : known) {
            if (n.equals(origin)) {
                routes.put(n, "");
            }
            routes.put(n, plot(origin, n, ht.get(n)) + "/");
        }
    }

    /**
     * Get list of errors encountered while finding paths.
     *
     * @return array of error descriptions
     */
    String[] getErrors() {
        return (errors.toArray(new String[0]));
    }

    /**
     * Get the route from this node to the specified node.
     *
     * @param destination node
     * @return list of node names separated by and ending with "/"
     */
    String getPath(String destination) {
        String ret = routes.get(destination);
        if (ret == null) {
            return ("");
        }
        return (ret);
    }

    private String plot(String from, String to, HashMap<String, Hop> info) {
        Hop nh = info.get(from);
        if (nh == null || nh.bad) {
            return (to);
        }
        if (nh.mark) {
            while (!nh.bad) {
                nh.bad = true;
                errors.add(nh.basis + " is part of a cycle");
                nh = info.get(nh.basis.getVia());
            }
            return (to);
        }
        nh.mark = true;
        String route = plot(nh.basis.getVia(), to, info);
        nh.mark = false;
        if (nh.bad) {
            return (to);
        }
        return (nh.basis.getVia() + "/" + route);
    }

    @Nullable
    private Hop getHop(HashSet<String> known, HashMap<String, HashMap<String, Hop>> ht, ProvHop ph) {
        if (!known.contains(ph.getFrom())) {
            errors.add(ph + " references unknown from node");
            return null;
        }
        if (!known.contains(ph.getTo())) {
            errors.add(ph + " references unknown destination node");
            return null;
        }
        HashMap<String, Hop> ht2 = ht.get(ph.getTo());
        Hop hop = ht2.get(ph.getFrom());
        if (hop != null) {
            hop.bad = true;
            errors.add(ph + " gives duplicate next hop - previous via was " + hop.basis.getVia());
            return null;
        }
        hop = new Hop();
        hop.basis = ph;
        ht2.put(ph.getFrom(), hop);
        if (!known.contains(ph.getVia())) {
            errors.add(ph + " references unknown via node");
            hop.bad = true;
            return null;
        }
        return hop;
    }

    private static class Hop {

        boolean mark;
        boolean bad;
        NodeConfig.ProvHop basis;
    }
}
