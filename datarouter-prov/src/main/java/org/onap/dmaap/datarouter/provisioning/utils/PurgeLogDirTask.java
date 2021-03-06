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


package org.onap.dmaap.datarouter.provisioning.utils;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;
import java.util.TimerTask;
import org.onap.dmaap.datarouter.provisioning.ProvRunner;

/**
 * This class provides a {@link TimerTask} that purges old logfiles (older than the number of days specified by the
 * org.onap.dmaap.datarouter.provserver.logretention property).
 *
 * @author Robert Eby
 * @version $Id: PurgeLogDirTask.java,v 1.2 2013/07/05 13:48:05 eby Exp $
 */
public class PurgeLogDirTask extends TimerTask {

    private static final long ONEDAY = 86400000L;

    private final String logdir;
    private final long interval;
    private EELFLogger utilsLogger;

    /**
     * PurgeLogDirTask constructor.
     */
    public PurgeLogDirTask() {
        Properties prop = ProvRunner.getProvProperties();
        logdir = prop.getProperty("org.onap.dmaap.datarouter.provserver.accesslog.dir");
        String str = prop.getProperty("org.onap.dmaap.datarouter.provserver.logretention", "30");
        this.utilsLogger = EELFManager.getInstance().getLogger("UtilsLog");
        long retention = 30;
        try {
            retention = Long.parseLong(str);
        } catch (NumberFormatException e) {
            // ignore
        }
        interval = retention * ONEDAY;
    }

    @Override
    public void run() {
        try {
            File dir = new File(logdir);
            if (dir.exists()) {
                purgeLogFiles(dir);
            }
        } catch (Exception e) {
            utilsLogger.error("Exception: " + e.getMessage(), e);
        }
    }

    private void purgeLogFiles(File dir) {
        long exptime = System.currentTimeMillis() - interval;
        for (File logfile : Objects.requireNonNull(dir.listFiles())) {
            if (logfile.lastModified() < exptime) {
                try {
                    Files.delete(logfile.toPath());
                } catch (IOException e) {
                    utilsLogger.error("Failed to delete file: " + logfile.getPath(), e);
                }
            }
        }
    }
}
