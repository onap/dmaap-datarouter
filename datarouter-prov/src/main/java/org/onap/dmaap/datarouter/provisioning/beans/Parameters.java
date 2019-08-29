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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.json.JSONObject;
import org.onap.dmaap.datarouter.provisioning.utils.DataSource;

/**
 * Methods to provide access to Provisioning parameters in the DB. This class also provides constants of the standard
 * parameters used by the Data Router.
 *
 * @author Robert Eby
 * @version $Id: Parameters.java,v 1.11 2014/03/12 19:45:41 eby Exp $
 */

public class Parameters extends Syncable {

    public static final String PROV_REQUIRE_SECURE = "PROV_REQUIRE_SECURE";
    public static final String PROV_REQUIRE_CERT = "PROV_REQUIRE_CERT";
    public static final String PROV_AUTH_ADDRESSES = "PROV_AUTH_ADDRESSES";
    public static final String PROV_AUTH_SUBJECTS = "PROV_AUTH_SUBJECTS";
    public static final String PROV_NAME = "PROV_NAME";
    public static final String PROV_ACTIVE_NAME = "PROV_ACTIVE_NAME";
    public static final String PROV_DOMAIN = "PROV_DOMAIN";
    public static final String PROV_MAXFEED_COUNT = "PROV_MAXFEED_COUNT";
    public static final String PROV_MAXSUB_COUNT = "PROV_MAXSUB_COUNT";
    public static final String PROV_POKETIMER1 = "PROV_POKETIMER1";
    public static final String PROV_POKETIMER2 = "PROV_POKETIMER2";
    public static final String PROV_SPECIAL_SUBNET = "PROV_SPECIAL_SUBNET";
    public static final String PROV_LOG_RETENTION = "PROV_LOG_RETENTION";
    public static final String DEFAULT_LOG_RETENTION = "DEFAULT_LOG_RETENTION";
    public static final String NODES = "NODES";
    public static final String ACTIVE_POD = "ACTIVE_POD";
    public static final String STANDBY_POD = "STANDBY_POD";
    public static final String LOGROLL_INTERVAL = "LOGROLL_INTERVAL";
    public static final String DELIVERY_INIT_RETRY_INTERVAL = "DELIVERY_INIT_RETRY_INTERVAL";
    public static final String DELIVERY_MAX_RETRY_INTERVAL = "DELIVERY_MAX_RETRY_INTERVAL";
    public static final String DELIVERY_RETRY_RATIO = "DELIVERY_RETRY_RATIO";
    public static final String DELIVERY_MAX_AGE = "DELIVERY_MAX_AGE";
    public static final String THROTTLE_FILTER = "THROTTLE_FILTER";
    public static final String STATIC_ROUTING_NODES = "STATIC_ROUTING_NODES";

    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private static final String SQLEXCEPTION = "SQLException: ";

    private String keyname;
    private String value;

    public Parameters(String key, String val) {
        this.keyname = key;
        this.value = val;
    }

    public Parameters(ResultSet rs) throws SQLException {
        this.keyname = rs.getString("KEYNAME");
        this.value = rs.getString("VALUE");
    }

    /**
     * Get all parameters in the DB as a Map.
     *
     * @return the Map of keynames/values from the DB.
     */
    public static Map<String, String> getParameters() {
        Map<String, String> props = new HashMap<>();
        for (Parameters p : getParameterCollection()) {
            props.put(p.getKeyname(), p.getValue());
        }
        return props;
    }

    /**
     * Method to get parameters.
     * @return collection of parameters
     */
    public static Collection<Parameters> getParameterCollection() {
        Collection<Parameters> coll = new ArrayList<>();
        String sql = "select * from PARAMETERS";
        try (Connection conn = DataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Parameters param = new Parameters(rs);
                    coll.add(param);
                }
            }
            DataSource.returnConnection(conn);
        } catch (SQLException e) {
            intlogger.error(SQLEXCEPTION + e.getMessage(), e);
        }
        return coll;
    }

    /**
     * Get a specific parameter value from the DB.
     *
     * @param key the key to lookup
     * @return the value, or null if non-existant
     */
    public static Parameters getParameter(String key) {
        Parameters val = null;
        String sql = "select KEYNAME, VALUE from PARAMETERS where KEYNAME = ?";
        try (Connection conn = DataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    val = new Parameters(rs);
                }
            }
            DataSource.returnConnection(conn);
        } catch (SQLException e) {
            intlogger.error(SQLEXCEPTION + e.getMessage(), e);
        }
        return val;
    }

    public String getKeyname() {
        return keyname;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public JSONObject asJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put("keyname", keyname);
        jo.put("value", value);
        return jo;
    }

    @Override
    public boolean doInsert(Connection conn) {
        boolean rv = true;
        String sql = "insert into PARAMETERS values (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, getKeyname());
            ps.setString(2, getValue());
            ps.execute();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0005 doInsert: " + e.getMessage(), e);
        }
        return rv;
    }

    @Override
    public boolean doUpdate(Connection conn) {
        boolean rv = true;
        String sql = "update PARAMETERS set VALUE = ? where KEYNAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, getValue());
            ps.setString(2, getKeyname());
            ps.executeUpdate();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0006 doUpdate: " + e.getMessage(),e);
        }
        return rv;
    }

    @Override
    public boolean doDelete(Connection conn) {
        boolean rv = true;
        String sql = "delete from PARAMETERS where KEYNAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, getKeyname());
            ps.execute();
        } catch (SQLException e) {
            rv = false;
            intlogger.warn("PROV0007 doDelete: " + e.getMessage(), e);
        }
        return rv;
    }

    @Override
    public String getKey() {
        return getKeyname();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Parameters)) {
            return false;
        }
        Parameters of = (Parameters) obj;
        return (!value.equals(of.value)) && (!keyname.equals(of.keyname));
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyname, value);
    }

    @Override
    public String toString() {
        return "PARAM: keyname=" + keyname + ", value=" + value;
    }
}

