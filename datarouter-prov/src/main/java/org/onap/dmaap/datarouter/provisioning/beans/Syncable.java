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

import java.sql.Connection;

import org.json.JSONObject;

/**
 * This abstract class defines the "contract" for beans that can be sync-ed with the database,
 * by means of straight comparison.  The <i>getKey</i> method is used to return the primary key
 * used to identify a record.
 *
 * @author Robert Eby
 * @version $Id: Syncable.java,v 1.1 2013/07/05 13:48:05 eby Exp $
 */
public abstract class Syncable implements Deleteable, Insertable, Updateable, JSONable {
	@Override
	abstract public JSONObject asJSONObject();

	@Override
	abstract public boolean doUpdate(Connection c);

	@Override
	abstract public boolean doInsert(Connection c);

	@Override
	abstract public boolean doDelete(Connection c);

	/**
	 * Get the "natural key" for this object type, as a String.
	 * @return the key
	 */
	abstract public String getKey();
}
