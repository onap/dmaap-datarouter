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
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.util.Iterator;
import org.onap.dmaap.datarouter.provisioning.utils.ProvDbUtils;
import org.onap.dmaap.datarouter.provisioning.utils.RLEBitSet;


/**
 * The representation of a Log Record, as retrieved from the DB.  Since this record format is only used to replicate
 * between provisioning servers, it is very bare-bones; e.g. there are no field setters and only 1 getter.
 *
 * @author Robert Eby
 * @version $Id: LogRecord.java,v 1.7 2014/03/12 19:45:41 eby Exp $
 */
public class LogRecord extends BaseLogRecord {

    private static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private final String type;
    private final String feedFileID;
    private final String remoteAddr;
    private final String user;
    private final int status;
    private final int subID;
    private final String fileID;
    private final int result;
    private final int attempts;
    private final String reason;
    private final long recordId;
    private final long clength2;
    private final String fileName;

    /**
     * LogRecord constructor.
     * @param rs ResultSet from SQL statement
     * @throws SQLException in case of SQL error
     */
    private LogRecord(ResultSet rs) throws SQLException {
        super(rs);
        this.type = rs.getString("TYPE");
        this.feedFileID = rs.getString("FEED_FILEID");
        this.remoteAddr = rs.getString("REMOTE_ADDR");
        this.user = rs.getString("USER");
        this.status = rs.getInt("STATUS");

        this.subID = rs.getInt("DELIVERY_SUBID");
        this.fileID = rs.getString("DELIVERY_FILEID");
        this.result = rs.getInt("RESULT");

        this.attempts = rs.getInt("ATTEMPTS");
        this.reason = rs.getString("REASON");

        this.recordId = rs.getLong("RECORD_ID");
        this.clength2 = rs.getLong("CONTENT_LENGTH_2");
        this.fileName = rs.getString("FILENAME");
    }

    /**
     * LogRecord Constructor from string array.
     * @param pp string array of LogRecord attributes
     * @throws ParseException in case of parse error
     */
    public LogRecord(String[] pp) throws ParseException {
        super(pp);
        this.type = pp[8];
        this.feedFileID = pp[9];
        this.remoteAddr = pp[10];
        this.user = pp[11];
        this.status = Integer.parseInt(pp[12]);

        this.subID = Integer.parseInt(pp[13]);
        this.fileID = pp[14];
        this.result = Integer.parseInt(pp[15]);

        this.attempts = Integer.parseInt(pp[16]);
        this.reason = pp[17];

        this.recordId = Long.parseLong(pp[18]);
        this.clength2 = (pp.length == 21) ? Long.parseLong(pp[19]) : 0;
        this.fileName = pp[20];
    }

    /**
     * Print all log records whose RECORD_IDs are in the bit set provided.
     *
     * @param os the {@link OutputStream} to print the records on
     * @param bs the {@link RLEBitSet} listing the record IDs to print
     * @throws IOException in case of I/O error
     */
    public static void printLogRecords(OutputStream os, RLEBitSet bs) throws IOException {
        Iterator<Long[]> iter = bs.getRangeIterator();
        try (Connection conn = ProvDbUtils.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "select * from LOG_RECORDS where RECORD_ID >= ? AND RECORD_ID <= ?")) {
            while (iter.hasNext()) {
                Long[] nxt = iter.next();
                ps.setLong(1, nxt[0]);
                ps.setLong(2, nxt[1]);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LogRecord lr = new LogRecord(rs);
                        os.write(lr.toString().getBytes());
                    }
                    ps.clearParameters();
                }
            }
        } catch (SQLException e) {
            intlogger.error("PROV0001 printLogRecords: " + e.getMessage(), e);
        }
    }

    public long getRecordId() {
        return recordId;
    }

    @Override
    public String toString() {
        return
            sdf.format(getEventTime()) + "|"
                + "LOG|"
                + getPublishId() + "|"
                + getFeedid() + "|"
                + getRequestUri() + "|"
                + getMethod() + "|"
                + getContentType() + "|"
                + getContentLength() + "|"
                + type + "|"
                + feedFileID + "|"
                + remoteAddr + "|"
                + user + "|"
                + status + "|"
                + subID + "|"
                + fileID + "|"
                + result + "|"
                + attempts + "|"
                + reason + "|"
                + recordId + "|"
                + clength2
                + "\n";
    }

    @Override
    public void load(PreparedStatement ps) throws SQLException {
        ps.setString(1, type);
        super.load(ps);                // loads fields 2-8
        if (type.equals("pub")) {
            ps.setString(9, feedFileID);
            ps.setString(10, remoteAddr);
            ps.setString(11, user);
            ps.setInt(12, status);
            ps.setNull(13, Types.INTEGER);
            ps.setNull(14, Types.VARCHAR);
            ps.setNull(15, Types.INTEGER);
            ps.setNull(16, Types.INTEGER);
            ps.setNull(17, Types.VARCHAR);
            ps.setLong(18, recordId);
            ps.setNull(19, Types.BIGINT);
            ps.setString(20, fileName);
        } else if (type.equals("del")) {
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.VARCHAR);
            ps.setString(11, user);
            ps.setNull(12, Types.INTEGER);
            ps.setInt(13, subID);
            ps.setString(14, fileID);
            ps.setInt(15, result);
            ps.setNull(16, Types.INTEGER);
            ps.setNull(17, Types.VARCHAR);
            ps.setLong(18, recordId);
            ps.setNull(19, Types.BIGINT);
            ps.setString(20, fileName);
        } else if (type.equals("exp")) {
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.VARCHAR);
            ps.setNull(11, Types.VARCHAR);
            ps.setNull(12, Types.INTEGER);
            ps.setInt(13, subID);
            ps.setString(14, fileID);
            ps.setNull(15, Types.INTEGER);
            ps.setInt(16, attempts);
            ps.setString(17, reason);
            ps.setLong(18, recordId);
            ps.setNull(19, Types.BIGINT);
            ps.setString(20, fileName);
        } else if (type.equals("pbf")) {
            ps.setString(9, feedFileID);
            ps.setString(10, remoteAddr);
            ps.setString(11, user);
            ps.setNull(12, Types.INTEGER);
            ps.setNull(13, Types.INTEGER);
            ps.setNull(14, Types.VARCHAR);
            ps.setNull(15, Types.INTEGER);
            ps.setNull(16, Types.INTEGER);
            ps.setNull(17, Types.VARCHAR);
            ps.setLong(18, recordId);
            ps.setLong(19, clength2);
            ps.setString(20, fileName);
        } else if (type.equals("dlx")) {
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.VARCHAR);
            ps.setNull(11, Types.VARCHAR);
            ps.setNull(12, Types.INTEGER);
            ps.setInt(13, subID);
            ps.setNull(14, Types.VARCHAR);
            ps.setNull(15, Types.INTEGER);
            ps.setNull(16, Types.INTEGER);
            ps.setNull(17, Types.VARCHAR);
            ps.setLong(18, recordId);
            ps.setLong(19, clength2);
            ps.setString(20, fileName);
        }
    }

    public static void main(String[] args) throws IOException {
        LogRecord.printLogRecords(System.out, new RLEBitSet(args[0]));
    }
}
