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

import org.apache.commons.lang3.StringUtils;
import org.onap.dmaap.datarouter.provisioning.utils.LOGJSONObject;


/**
 * The representation of a Publish Record, as retrieved from the DB.
 *
 * @author Robert Eby
 * @version $Id: PublishRecord.java,v 1.6 2013/10/28 18:06:53 eby Exp $
 */

public class PublishRecord extends BaseLogRecord {

    public static final String STATUS_CODE = "statusCode";
    public static final String SOURCE_IP = "sourceIP";
    public static final String ENDPOINT_ID = "endpointId";
    public static final String FILE_NAME = "fileName";
    private String feedFileid;
    private String remoteAddr;
    private String user;
    private int status;
    private String fileName;

    /**
     * Publish record constructor.
     * @param pp string array of attributes
     * @throws ParseException in case of parse error
     */
    public PublishRecord(String[] pp) throws ParseException {
        super(pp);
        int ix = pp[4].indexOf("/publish/");
        if (ix < 0) {
            throw new ParseException("bad pattern", 0);
        }
        ix = pp[4].indexOf('/', ix + 9);
        if (ix < 0) {
            throw new ParseException("bad pattern", 0);
        }
        this.feedFileid = pp[4].substring(ix + 1);
        this.remoteAddr = pp[8];
        this.user = pp[9];
        this.status = Integer.parseInt(pp[10]);
        this.fileName = StringUtils.substringAfterLast(this.getRequestUri(), "/");
    }

    /**
     * Publish record constructor.
     * @param rs ResultSet from DB
     * @throws SQLException in case of SQL error
     */
    public PublishRecord(ResultSet rs) throws SQLException {
        super(rs);
        this.feedFileid = rs.getString("FEED_FILEID");
        this.remoteAddr = rs.getString("REMOTE_ADDR");
        this.user = rs.getString("USER");
        this.status = rs.getInt("STATUS");
        this.fileName = rs.getString("FILENAME");
    }

    public String getFeedFileid() {
        return feedFileid;
    }

    public void setFeedFileid(String feedFileid) {
        this.feedFileid = feedFileid;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    /**
     * Method to reorder json object.
     * @param jo LOGJSONObject
     * @return LOGJSONObject
     */
    public LOGJSONObject reOrderObject(LOGJSONObject jo) {
        LinkedHashMap<String, Object> logrecordObj = new LinkedHashMap<>();


        logrecordObj.put(STATUS_CODE, jo.get(STATUS_CODE));
        logrecordObj.put("publishId", jo.get("publishId"));
        logrecordObj.put("requestURI", jo.get("requestURI"));
        logrecordObj.put(SOURCE_IP, jo.get(SOURCE_IP));
        logrecordObj.put("method", jo.get("method"));
        logrecordObj.put("contentType", jo.get("contentType"));
        logrecordObj.put(ENDPOINT_ID, jo.get(ENDPOINT_ID));
        logrecordObj.put("type", jo.get("type"));
        logrecordObj.put("date", jo.get("date"));
        logrecordObj.put("contentLength", jo.get("contentLength"));
        logrecordObj.put(FILE_NAME, jo.get(FILE_NAME));

        return new LOGJSONObject(logrecordObj);
    }


    @Override
    public LOGJSONObject asJSONObject() {
        LOGJSONObject jo = super.asJSONObject();
        jo.put("type", "pub");
        jo.put("feedFileid", feedFileid);
        jo.put("remoteAddr", remoteAddr);
        jo.put("user", user);
        jo.put(SOURCE_IP, remoteAddr);
        jo.put(ENDPOINT_ID, user);
        jo.put(STATUS_CODE, status);
        jo.put(FILE_NAME, fileName);

        return this.reOrderObject(jo);
    }

    @Override
    public void load(PreparedStatement ps) throws SQLException {
        ps.setString(1, "pub");        // field 1: type
        super.load(ps);                // loads fields 2-8
        ps.setString(9, getFeedFileid());
        ps.setString(10, getRemoteAddr());
        ps.setString(11, getUser());
        ps.setInt(12, getStatus());
        ps.setNull(13, Types.INTEGER);
        ps.setNull(14, Types.VARCHAR);
        ps.setNull(15, Types.INTEGER);
        ps.setNull(16, Types.INTEGER);
        ps.setNull(17, Types.VARCHAR);
        ps.setNull(19, Types.BIGINT);
        ps.setString(20, getFileName());
    }
}
