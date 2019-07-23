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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.onap.dmaap.datarouter.provisioning.utils.DB;

/**
 * This class is used to aid in the mapping of node names from/to node IDs.
 *
 * @author Robert P. Eby
 * @version $Id: NodeClass.java,v 1.2 2014/01/15 16:08:43 eby Exp $
 */
public abstract class NodeClass extends Syncable {

    private static final String PROV_0005_DO_INSERT = "PROV0005 doInsert: ";
    private static Map<String, Integer> map;
    private static EELFLogger intLogger = EELFManager.getInstance().getLogger("InternalLog");

    NodeClass() {
        // init on first use
        if (map == null) {
            reload();
        }
    }

    /**
     * Add nodes to the NODES table, when the NODES parameter value is changed. Nodes are only added to the table, they
     * are never deleted.  The node name is normalized to contain the domain (if missing).
     *
     * @param nodes a pipe separated list of the current nodes
     */
    public static void setNodes(String[] nodes) {
        if (map == null) {
            reload();
        }
        int nextid = 0;
        for (Integer n : map.values()) {
            if (n >= nextid) {
                nextid = n + 1;
            }
        }
        // take | separated list, add domain if needed.

        for (String node : nodes) {
            node = normalizeNodename(node);
            if (!map.containsKey(node)) {
                intLogger.info("..adding " + node + " to NODES with index " + nextid);
                map.put(node, nextid);
                insertNodesToTable(nextid, node);
                nextid++;
            }
        }
    }

    private static void insertNodesToTable(int nextid, String node) {
        DB db = new DB();
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn
                    .prepareStatement("insert into NODES (NODEID, NAME, ACTIVE) values (?, ?, 1)")) {
                ps.setInt(1, nextid);
                ps.setString(2, node);
                ps.execute();
            } finally {
                db.release(conn);
            }
        } catch (SQLException e) {
            intLogger.error(PROV_0005_DO_INSERT + e.getMessage(), e);
        }
    }

    private static void reload() {
        Map<String, Integer> m = new HashMap<>();
        String sql = "select NODEID, NAME from NODES";
        DB db = new DB();
        try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("NODEID");
                    String name = rs.getString("NAME");
                    m.put(name, id);
                }
            } finally {
                db.release(conn);
            }
        } catch (SQLException e) {
            intLogger.error(PROV_0005_DO_INSERT + e.getMessage(),e);
        }
        map = m;
    }

    static Integer lookupNodeName(final String name) {
        Integer n = map.get(name);
        if (n == null) {
            throw new IllegalArgumentException("Invalid node name: " + name);
        }
        return n;
    }

    public static Collection<String> lookupNodeNames(String patt) {
        Collection<String> coll = new TreeSet<>();
        final Set<String> keyset = map.keySet();
        for (String s : patt.toLowerCase().split(",")) {
            if (s.endsWith("*")) {
                addNodeToCollection(coll, keyset, s);
            } else if (keyset.contains(s)) {
                coll.add(s);
            } else if (keyset.contains(normalizeNodename(s))) {
                coll.add(normalizeNodename(s));
            } else {
                throw new IllegalArgumentException("Invalid node name: " + s);
            }
        }
        return coll;
    }

    private static void addNodeToCollection(Collection<String> coll, Set<String> keyset, String s) {
        s = s.substring(0, s.length() - 1);
        for (String s2 : keyset) {
            if (s2.startsWith(s)) {
                coll.add(s2);
            }
        }
    }

    public static String normalizeNodename(String s) {
        if (s != null && s.indexOf('.') <= 0) {
            Parameters p = Parameters.getParameter(Parameters.PROV_DOMAIN);
            if (p != null) {
                String domain = p.getValue();
                s += "." + domain;
            }
            return s.toLowerCase();
        } else {
            return s;
        }

    }

    String lookupNodeID(int n) {
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() == n) {
                return entry.getKey();
            }
        }
        return null;
    }
}
