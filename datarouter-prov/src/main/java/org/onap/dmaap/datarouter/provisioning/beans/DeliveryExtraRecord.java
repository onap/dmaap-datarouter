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
 * The representation of a Delivery Extra (DLX) Record, as retrieved from the DB.
 * @author Robert Eby
 * @version $Id: DeliveryExtraRecord.java,v 1.1 2013/10/28 18:06:52 eby Exp $
 */
public class DeliveryExtraRecord extends BaseLogRecord {
    private int  subid;
    private long contentLength2;

    public DeliveryExtraRecord(String[] pp) throws ParseException {
        super(pp);
        this.subid = Integer.parseInt(pp[4]);
        this.contentLength2 = Long.parseLong(pp[6]);
    }
    public DeliveryExtraRecord(ResultSet rs) throws SQLException {
        super(rs);
        // Note: because this record should be "rare" these fields are mapped to unconventional fields in the DB
        this.subid  = rs.getInt("DELIVERY_SUBID");
        this.contentLength2 = rs.getInt("CONTENT_LENGTH_2");
    }
    @Override
    public void load(PreparedStatement ps) throws SQLException {
        ps.setString(1, "dlx");        // field 1: type
        super.load(ps);                // loads fields 2-8
        ps.setNull( 9, Types.VARCHAR);
        ps.setNull(10, Types.VARCHAR);
        ps.setNull(11, Types.VARCHAR);
        ps.setNull(12, Types.INTEGER);
        ps.setInt (13, subid);
        ps.setNull(14, Types.VARCHAR);
        ps.setNull(15, Types.INTEGER);
        ps.setNull(16, Types.INTEGER);
        ps.setNull(17, Types.VARCHAR);
        ps.setLong(19, contentLength2);
    }
}
