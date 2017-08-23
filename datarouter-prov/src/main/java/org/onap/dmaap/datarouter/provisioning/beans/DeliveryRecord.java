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

import org.json.LOGJSONObject;

/**
 * The representation of a Delivery Record, as retrieved from the DB.
 * @author Robert Eby
 * @version $Id: DeliveryRecord.java,v 1.9 2014/03/12 19:45:41 eby Exp $
 */
public class DeliveryRecord extends BaseLogRecord {
	private int subid;
	private String fileid;
	private int result;
	private String user;

	public DeliveryRecord(String[] pp) throws ParseException {
		super(pp);
		String fileid = pp[5];
		if (fileid.lastIndexOf('/') >= 0)
			fileid = fileid.substring(fileid.lastIndexOf('/')+1);
		this.subid  = Integer.parseInt(pp[4]);
		this.fileid = fileid;
		this.result = Integer.parseInt(pp[10]);
		this.user   = pp[9];
		if (this.user != null && this.user.length() > 50)
			this.user = this.user.substring(0, 50);
	}
	public DeliveryRecord(ResultSet rs) throws SQLException {
		super(rs);
		this.subid  = rs.getInt("DELIVERY_SUBID");
		this.fileid = rs.getString("DELIVERY_FILEID");
		this.result = rs.getInt("RESULT");
		this.user   = rs.getString("USER");
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
	public int getResult() {
		return result;
	}
	public void setResult(int result) {
		this.result = result;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	
	
	public LOGJSONObject reOrderObject(LOGJSONObject jo) {
		LinkedHashMap<String,Object> logrecordObj = new LinkedHashMap<String,Object>();
		
		logrecordObj.put("statusCode", jo.get("statusCode"));
		logrecordObj.put("deliveryId", jo.get("deliveryId"));
		logrecordObj.put("publishId", jo.get("publishId"));
		logrecordObj.put("requestURI", jo.get("requestURI"));
		//logrecordObj.put("sourceIP", jo.get("sourceIP"));
		logrecordObj.put("method", jo.get("method"));
		logrecordObj.put("contentType", jo.get("contentType"));
		//logrecordObj.put("endpointId", jo.get("endpointId"));
		logrecordObj.put("type", jo.get("type"));
		logrecordObj.put("date", jo.get("date"));
		logrecordObj.put("contentLength", jo.get("contentLength"));


		LOGJSONObject newjo = new LOGJSONObject(logrecordObj);
		return newjo;
	}
	
	@Override
	public LOGJSONObject asJSONObject() {
		LOGJSONObject jo = super.asJSONObject();
		jo.put("type", "del");
		jo.put("deliveryId", user);
		jo.put("statusCode", result);
		
		LOGJSONObject newjo = this.reOrderObject(jo);
		return newjo;
	}
	@Override
	public void load(PreparedStatement ps) throws SQLException {
		ps.setString(1, "del");		// field 1: type
		super.load(ps);				// loads fields 2-8
		ps.setNull  (9,  Types.VARCHAR);
		ps.setNull  (10, Types.VARCHAR);
		ps.setString(11, getUser());
		ps.setNull  (12, Types.INTEGER);
		ps.setInt   (13, getSubid());
		ps.setString(14, getFileid());
		ps.setInt   (15, getResult());
		ps.setNull  (16, Types.INTEGER);
		ps.setNull  (17, Types.VARCHAR);
		ps.setNull  (19, Types.BIGINT);
	}
}
