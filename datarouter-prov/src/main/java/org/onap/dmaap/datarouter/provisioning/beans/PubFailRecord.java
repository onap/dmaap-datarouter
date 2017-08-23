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

/**
 * The representation of a Publish Failure (PBF) Record, as retrieved from the DB.
 * @author Robert Eby
 * @version $Id: PubFailRecord.java,v 1.1 2013/10/28 18:06:53 eby Exp $
 */
public class PubFailRecord extends BaseLogRecord {
	private long contentLengthReceived;
	private String sourceIP;
	private String user;
	private String error;

	public PubFailRecord(String[] pp) throws ParseException {
		super(pp);
		this.contentLengthReceived = Long.parseLong(pp[8]);
		this.sourceIP = pp[9];
		this.user     = pp[10];
		this.error    = pp[11];
	}
	public PubFailRecord(ResultSet rs) throws SQLException {
		super(rs);
		// Note: because this record should be "rare" these fields are mapped to unconventional fields in the DB
		this.contentLengthReceived = rs.getLong("CONTENT_LENGTH_2");
		this.sourceIP = rs.getString("REMOTE_ADDR");
		this.user     = rs.getString("USER");
		this.error    = rs.getString("FEED_FILEID");
	}
	public long getContentLengthReceived() {
		return contentLengthReceived;
	}
	public String getSourceIP() {
		return sourceIP;
	}
	public String getUser() {
		return user;
	}
	public String getError() {
		return error;
	}
	@Override
	public void load(PreparedStatement ps) throws SQLException {
		ps.setString(1, "pbf");		// field 1: type
		super.load(ps);				// loads fields 2-8
		ps.setString( 9, getError());
		ps.setString(10, getSourceIP());
		ps.setString(11, getUser());
		ps.setNull  (12, Types.INTEGER);
		ps.setNull  (13, Types.INTEGER);
		ps.setNull  (14, Types.VARCHAR);
		ps.setNull  (15, Types.INTEGER);
		ps.setNull  (16, Types.INTEGER);
		ps.setNull  (17, Types.VARCHAR);
		ps.setLong  (19, getContentLengthReceived());
	}
}
