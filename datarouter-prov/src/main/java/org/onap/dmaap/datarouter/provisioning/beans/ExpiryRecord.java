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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.util.LinkedHashMap;

import org.onap.dmaap.datarouter.provisioning.utils.LOGJSONObject;


/**
 * The representation of a Expiry Record, as retrieved from the DB.
 *
 * @author Robert Eby
 * @version $Id: ExpiryRecord.java,v 1.4 2013/10/28 18:06:52 eby Exp $
 */
public class ExpiryRecord extends BaseLogRecord {

    public static final String EXPIRY_REASON = "expiryReason";
    public static final String ATTEMPTS = "attempts";
    private int subid;
    private String fileid;
    private int deliveryAttempts;
    private String reason;

    /**
     * ExpiryRecord constructor.
     * @param pp string array of ExpiryRecord attributes
     * @throws ParseException in case of parse error
     */
    public ExpiryRecord(String[] pp) throws ParseException {
        super(pp);
        String thisFileid = pp[5];
        if (thisFileid.lastIndexOf('/') >= 0) {
            thisFileid = thisFileid.substring(thisFileid.lastIndexOf('/') + 1);
        }
        this.subid = Integer.parseInt(pp[4]);
        this.fileid = thisFileid;
        this.deliveryAttempts = Integer.parseInt(pp[10]);
        this.reason = pp[9];
        if (!"notRetryable".equals(reason) && !"retriesExhausted".equals(reason) && !"diskFull".equals(reason)) {
            this.reason = "other";
        }
    }

    /**
     * ExpiryRecord constructor from ResultSet.
     * @param rs ResultSet of ExpiryREcord attributes
     * @throws SQLException in case of error with SQL statement
     */
    public ExpiryRecord(ResultSet rs) throws SQLException {
        super(rs);
        this.subid = rs.getInt("DELIVERY_SUBID");
        this.fileid = rs.getString("DELIVERY_FILEID");
        this.deliveryAttempts = rs.getInt("ATTEMPTS");
        this.reason = rs.getString("REASON");
    }

    public int getSubid() {
        return subid;
    }

    public void setSubid(int subid) {
        this.subid = subid;
    }

    public String getFileid() {
        return fileid;
    }

    public void setFileid(String fileid) {
        this.fileid = fileid;
    }

    public int getDeliveryAttempts() {
        return deliveryAttempts;
    }

    public void setDeliveryAttempts(int deliveryAttempts) {
        this.deliveryAttempts = deliveryAttempts;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Method to reorder LOGJSONObject.
     * @param jo LOGJSONObject
     * @return LOGJSONObject
     */
    public LOGJSONObject reOrderObject(LOGJSONObject jo) {
        LinkedHashMap<String, Object> logrecordObj = new LinkedHashMap<>();

        logrecordObj.put(EXPIRY_REASON, jo.get(EXPIRY_REASON));
        logrecordObj.put("publishId", jo.get("publishId"));
        logrecordObj.put(ATTEMPTS, jo.get(ATTEMPTS));
        logrecordObj.put("requestURI", jo.get("requestURI"));
        logrecordObj.put("method", jo.get("method"));
        logrecordObj.put("contentType", jo.get("contentType"));
        logrecordObj.put("type", jo.get("type"));
        logrecordObj.put("date", jo.get("date"));
        logrecordObj.put("contentLength", jo.get("contentLength"));

        return new LOGJSONObject(logrecordObj);
    }

    @Override
    public LOGJSONObject asJSONObject() {
        LOGJSONObject jo = super.asJSONObject();
        jo.put("type", "exp");
        jo.put(EXPIRY_REASON, reason);
        jo.put(ATTEMPTS, deliveryAttempts);

        return reOrderObject(jo);
    }

    @Override
    public void load(PreparedStatement ps) throws SQLException {
        ps.setString(1, "exp");        // field 1: type
        super.load(ps);                // loads fields 2-8
        ps.setNull(9, Types.VARCHAR);
        ps.setNull(10, Types.VARCHAR);
        ps.setNull(11, Types.VARCHAR);
        ps.setNull(12, Types.INTEGER);
        ps.setInt(13, getSubid());
        ps.setString(14, getFileid());
        ps.setNull(15, Types.INTEGER);
        ps.setInt(16, getDeliveryAttempts());
        ps.setString(17, getReason());
        ps.setNull(19, Types.BIGINT);
        ps.setNull(20, Types.VARCHAR);
    }
}
