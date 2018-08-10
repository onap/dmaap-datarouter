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
import java.sql.SQLException;

import org.onap.dmaap.datarouter.provisioning.utils.LogfileLoader;

/**
 * This interface is used by bean classes that can be loaded into the LOG_RECORDS table using the
 * PreparedStatement at {@link LogfileLoader}.INSERT_SQL.
 *
 * @author Robert Eby
 * @version $Id: Loadable.java,v 1.2 2013/08/06 13:28:33 eby Exp $
 */
public interface Loadable {
    /**
     * Load the 18 fields in the PreparedStatement <i>ps</i>. The fields are:
     * <ol>
     * <li>type (String)</li>
     * <li>event_time (long)</li>
     * <li>publish ID (String)</li>
     * <li>feed ID (int)</li>
     * <li>request URI (String)</li>
     * <li>method (String)</li>
     * <li>content type (String)</li>
     * <li>content length (long)</li>
     * <li>feed File ID (String)</li>
     * <li>remote address (String)</li>
     * <li>user (String)</li>
     * <li>status (int)</li>
     * <li>delivery subscriber id (int)</li>
     * <li>delivery File ID (String)</li>
     * <li>result (int)</li>
     * <li>attempts (int)</li>
     * <li>reason (String)</li>
     * <li>record ID (long)</li>
     * </ol>
     *
     * @param ps the PreparedStatement to load
     */
    public void load(PreparedStatement ps) throws SQLException;
}
