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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.onap.dmaap.datarouter.provisioning.utils.LOGJSONObject;

/**
 * Define the common fields used by the three types of records generated by DR nodes.
 *
 * @author Robert Eby
 * @version $Id: BaseLogRecord.java,v 1.10 2013/10/29 16:57:57 eby Exp $
 */
public class BaseLogRecord implements LOGJSONable, Loadable {
    protected static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private long eventTime;
    private String publishId;
    private int feedid;
    private String requestUri;
    private String method;
    private String contentType;
    private long contentLength;

    protected BaseLogRecord(String[] pp) throws ParseException {

        Date dt = parseDate(pp[0]);
        this.eventTime     = dt.getTime();
        this.publishId     = pp[2];
        this.feedid        = Integer.parseInt(pp[3]);
        if ("DLX".equals(pp[1])) {
            this.requestUri    = "";
            this.method        = "GET";    // Note: we need a valid value in this field, even though unused
            this.contentType   = "";
            this.contentLength = Long.parseLong(pp[5]);
        } else  if ("PUB".equals(pp[1]) || "LOG".equals(pp[1]) || "PBF".equals(pp[1])) {
            this.requestUri    = pp[4];
            this.method        = pp[5];
            this.contentType   = pp[6];
            this.contentLength = Long.parseLong(pp[7]);
        } else {
            this.requestUri    = pp[5];
            this.method        = pp[6];
            this.contentType   = pp[7];
            this.contentLength = Long.parseLong(pp[8]);
        }
    }

    protected BaseLogRecord(ResultSet rs) throws SQLException {
        this.eventTime     = rs.getLong("EVENT_TIME");
        this.publishId     = rs.getString("PUBLISH_ID");
        this.feedid        = rs.getInt("FEEDID");
        this.requestUri    = rs.getString("REQURI");
        this.method        = rs.getString("METHOD");
        this.contentType   = rs.getString("CONTENT_TYPE");
        this.contentLength = rs.getLong("CONTENT_LENGTH");
    }

    protected Date parseDate(final String str) throws ParseException {
        int[] num = new int[7];
        int place = 0;
        for (int i = 0; i < str.length(); i++) {
            char chr = str.charAt(i);
            if (chr < '0' || chr > '9') {
                place++;
            } else {
                if (place > num.length) {
                    throw new ParseException("parseDate()", 0);
                }
                num[place] = (num[place] * 10) + (chr - '0');
            }
        }
        if (place != 7) {
            throw new ParseException("parseDate()", 1);
        }
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, num[0]);
        cal.set(Calendar.MONTH, num[1] - 1);
        cal.set(Calendar.DAY_OF_MONTH, num[2]);
        cal.set(Calendar.HOUR_OF_DAY, num[3]);
        cal.set(Calendar.MINUTE, num[4]);
        cal.set(Calendar.SECOND, num[5]);
        cal.set(Calendar.MILLISECOND, num[6]);
        return cal.getTime();
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public String getPublishId() {
        return publishId;
    }

    public void setPublishId(String publishId) {
        this.publishId = publishId;
    }

    public int getFeedid() {
        return feedid;
    }

    public void setFeedid(int feedid) {
        this.feedid = feedid;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    public LOGJSONObject asJSONObject() {
        LOGJSONObject jo = new LOGJSONObject();
        String str = "";
        synchronized (sdf) {
            str = sdf.format(eventTime);
        }
        jo.put("date", str);
        jo.put("publishId", publishId);
        jo.put("requestURI", requestUri);
        jo.put("method", method);
        if (method.equals("PUT")) {
            jo.put("contentType", contentType);
            jo.put("contentLength", contentLength);
        }
        return jo;
    }

    @Override
    public void load(PreparedStatement ps) throws SQLException {
        ps.setLong(2, getEventTime());
        ps.setString(3, getPublishId());
        ps.setInt(4, getFeedid());
        ps.setString(5, getRequestUri());
        ps.setString(6, getMethod());
        ps.setString(7, getContentType());
        ps.setLong(8, getContentLength());
    }
}

