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

package org.onap.dmaap.datarouter.reports;

import org.apache.log4j.Logger;

/**
 * Base class for all the report generating classes.
 *
 * @author Robert P. Eby
 * @version $Id: ReportBase.java,v 1.1 2013/10/28 18:06:53 eby Exp $
 */
abstract public class ReportBase implements Runnable {
    protected long from, to;
    protected String outfile;
    protected Logger logger;

    public ReportBase() {
        this.from = 0;
        this.to = System.currentTimeMillis();
        this.logger = Logger.getLogger("org.onap.dmaap.datarouter.reports");
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public String getOutfile() {
        return outfile;
    }

    public void setOutputFile(String s) {
        this.outfile = s;
    }

    @Override
    abstract public void run();
}
