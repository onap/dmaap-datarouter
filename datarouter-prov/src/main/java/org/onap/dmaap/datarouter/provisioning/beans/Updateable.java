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

/**
 * An object that can be UPDATE-ed in the database.
 *
 * @author Robert Eby
 * @version $Id: Updateable.java,v 1.2 2013/05/29 14:44:36 eby Exp $
 */
public interface Updateable {
    /**
     * Update this object in the DB.
     *
     * @param c the JDBC Connection to use
     * @return true if the UPDATE succeeded, false otherwise
     */
    boolean doUpdate(Connection c);
}
