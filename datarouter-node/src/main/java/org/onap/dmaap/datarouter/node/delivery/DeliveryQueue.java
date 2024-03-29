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


package org.onap.dmaap.datarouter.node.delivery;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.onap.dmaap.datarouter.node.DestInfo;
import org.onap.dmaap.datarouter.node.log.StatusLog;

/**
 * Mechanism for monitoring and controlling delivery of files to a destination.
 *
 * <p>The DeliveryQueue class maintains lists of DeliveryTasks for a single
 * destination (a subscription or another data router node) and assigns
 * delivery threads to try to deliver them.  It also maintains a delivery
 * status that causes it to back off on delivery attempts after a failure.
 *
 * <p>If the most recent delivery result was a failure, then no more attempts
 * will be made for a period of time.  Initially, and on the first failure
 * following a success, this delay will be DeliveryQueueHelper.getInitFailureTimer() (milliseconds).
 * If, after this delay, additional failures occur, each failure will
 * multiply the delay by DeliveryQueueHelper.getFailureBackoff() up to a
 * maximum delay specified by DeliveryQueueHelper.getMaxFailureTimer().
 * Note that this behavior applies to the delivery queue as a whole and not
 * to individual files in the queue.  If multiple files are being
 * delivered and one fails, the delay will be started.  If a second
 * delivery fails while the delay was active, it will not change the delay
 * or change the duration of any subsequent delay.
 * If, however, it succeeds, it will cancel the delay.
 * The queue maintains 3 collections of files to deliver: A todoList of
 * files that will be attempted, a working set of files that are being
 * attempted, and a retry set of files that were attempted and failed.
 * Whenever the todoList is empty and needs to be refilled, a scan of the
 * spool directory is made and the file names sorted.  Any files in the working set are ignored.
 * If a DeliveryTask for the file is in the retry set, then that delivery
 * task is placed on the todoList.  Otherwise, a new DeliveryTask for the
 * file is created and placed on the todoList.
 * If, when a DeliveryTask is about to be removed from the todoList, its
 * age exceeds DeliveryQueueHelper.getExpirationTimer(), then it is instead
 * marked as expired.
 *
 * <p>A delivery queue also maintains a skip flag.  This flag is true if the
 * failure timer is active or if no files are found in a directory scan.
 */
public class DeliveryQueue implements Runnable, DeliveryTaskHelper {
    private static EELFLogger logger = EELFManager.getInstance().getLogger(DeliveryQueue.class);
    private DeliveryQueueHelper deliveryQueueHelper;

    private DestInfo destinationInfo;
    private HashMap<String, DeliveryTask> working = new HashMap<>();
    private HashMap<String, DeliveryTask> retry = new HashMap<>();
    private int todoindex;
    private boolean failed;
    private long failduration;
    private long resumetime;
    private File dir;
    private List<DeliveryTask> todoList = new ArrayList<>();

    /**
     * Create a delivery queue for a given destination info.
     */
    public DeliveryQueue(DeliveryQueueHelper deliveryQueueHelper, DestInfo destinationInfo) {
        this.deliveryQueueHelper = deliveryQueueHelper;
        this.destinationInfo = destinationInfo;
        dir = new File(destinationInfo.getSpool());
        dir.mkdirs();
    }

    /**
     * Try to cancel a delivery task.
     *
     * @return The length of the task in bytes or 0 if the task cannot be cancelled.
     */
    public synchronized long cancelTask(String pubid) {
        if (working.get(pubid) != null) {
            return (0);
        }
        DeliveryTask dt = retry.get(pubid);
        if (dt == null) {
            for (int i = todoindex; i < todoList.size(); i++) {
                DeliveryTask xdt = todoList.get(i);
                if (xdt.getPublishId().equals(pubid)) {
                    dt = xdt;
                    break;
                }
            }
        }
        if (dt == null) {
            dt = new DeliveryTask(this, pubid);
            if (dt.getFileId() == null) {
                return (0);
            }
        }
        if (dt.isCleaned()) {
            return (0);
        }
        StatusLog.logExp(dt.getPublishId(), dt.getFeedId(), dt.getSubId(), dt.getURL(),
                dt.getMethod(), dt.getCType(), dt.getLength(), "diskFull", dt.getAttempts());
        dt.clean();
        return (dt.getLength());
    }

    /**
     * Mark that a delivery task has succeeded.
     */
    private synchronized void markSuccess(DeliveryTask task) {
        working.remove(task.getPublishId());
        logger.info(task.getPublishId() + " marked as success.");
        task.clean();
        failed = false;
        failduration = 0;
    }

    /**
     * Mark that a delivery task has expired.
     */
    private synchronized void markExpired(DeliveryTask task) {
        logger.info(task.getPublishId() + " marked as expired.");
        task.clean();
    }

    /**
     * Mark that a delivery task has failed permanently.
     */
    private synchronized void markFailNoRetry(DeliveryTask task) {
        working.remove(task.getPublishId());
        logger.info(task.getPublishId() + " marked as failed permanently");
        task.clean();
        failed = false;
        failduration = 0;
    }

    private void fdupdate() {
        if (!failed) {
            failed = true;
            if (failduration == 0) {
                if (destinationInfo.isPrivilegedSubscriber()) {
                    failduration = deliveryQueueHelper.getWaitForFileProcessFailureTimer();
                } else {
                    failduration = deliveryQueueHelper.getInitFailureTimer();
                }
            }
            resumetime = System.currentTimeMillis() + failduration;
            long maxdur = deliveryQueueHelper.getMaxFailureTimer();
            failduration = (long) (failduration * deliveryQueueHelper.getFailureBackoff());
            if (failduration > maxdur) {
                failduration = maxdur;
            }
        }
    }

    /**
     * Mark that a delivery task has been redirected.
     */
    private synchronized void markRedirect(DeliveryTask task) {
        working.remove(task.getPublishId());
        logger.info(task.getPublishId() + " marked as redirected.");
        retry.put(task.getPublishId(), task);
    }

    /**
     * Mark that a delivery task has temporarily failed.
     */
    private synchronized void markFailWithRetry(DeliveryTask task) {
        working.remove(task.getPublishId());
        logger.info(task.getPublishId() + " marked as temporarily failed.");
        retry.put(task.getPublishId(), task);
        fdupdate();
    }

    /**
     * Get the next task.
     */
    public synchronized DeliveryTask getNext() {
        DeliveryTask ret = peekNext();
        if (ret != null) {
            todoindex++;
            working.put(ret.getPublishId(), ret);
        }
        return (ret);
    }

    /**
     * Peek at the next task.
     */
    public synchronized DeliveryTask peekNext() {
        long now = System.currentTimeMillis();
        long mindate = now - deliveryQueueHelper.getExpirationTimer();
        if (failed) {
            if (now > resumetime) {
                failed = false;
            } else {
                return (null);
            }
        }
        while (true) {
            if (todoindex >= todoList.size()) {
                todoindex = 0;
                todoList = new ArrayList<>();
                String[] files = dir.list();
                if (files != null) {
                    Arrays.sort(files);
                    scanForNextTask(files);
                }
                retry = new HashMap<>();
            }
            return getDeliveryTask(mindate);
        }
    }

    /**
     * Update the destination info for this delivery queue.
     */
    public void config(DestInfo destinationInfo) {
        this.destinationInfo = destinationInfo;
    }

    /**
     * Get the dest info.
     */
    public DestInfo getDestinationInfo() {
        return (destinationInfo);
    }

    /**
     * Get the config manager.
     */
    public DeliveryQueueHelper getConfig() {
        return (deliveryQueueHelper);
    }

    /**
     * Exceptional condition occurred during delivery.
     */
    public void reportDeliveryExtra(DeliveryTask task, long sent) {
        StatusLog.logDelExtra(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getLength(), sent);
    }

    /**
     * Message too old to deliver.
     */
    void reportExpiry(DeliveryTask task) {
        StatusLog.logExp(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(),
                task.getCType(), task.getLength(), "retriesExhausted", task.getAttempts());
        markExpired(task);
    }

    /**
     * Completed a delivery attempt.
     */
    public void reportStatus(DeliveryTask task, int status, String xpubid, String location) {
        if (status < 300) {
            StatusLog.logDel(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(),
                    task.getCType(), task.getLength(), destinationInfo.getAuthUser(), status, xpubid);
            if (destinationInfo.isPrivilegedSubscriber()) {
                task.setResumeTime(System.currentTimeMillis()
                                           + deliveryQueueHelper.getWaitForFileProcessFailureTimer());
                markFailWithRetry(task);
            } else {
                markSuccess(task);
            }
        } else if (status < 400 && deliveryQueueHelper.isFollowRedirects()) {
            StatusLog.logDel(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(),
                    task.getCType(), task.getLength(), destinationInfo.getAuthUser(), status, location);
            if (deliveryQueueHelper.handleRedirection(destinationInfo, location, task.getFileId())) {
                markRedirect(task);
            } else {
                StatusLog.logExp(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(),
                        task.getMethod(), task.getCType(), task.getLength(), "notRetryable", task.getAttempts());
                markFailNoRetry(task);
            }
        } else if (status < 500 && status != 429) {
            // Status 429 is the standard response for Too Many Requests and indicates
            // that a file needs to be delivered again at a later time.
            StatusLog.logDel(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(),
                    task.getCType(), task.getLength(), destinationInfo.getAuthUser(), status, location);
            StatusLog.logExp(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(),
                    task.getCType(), task.getLength(), "notRetryable", task.getAttempts());
            markFailNoRetry(task);
        } else {
            StatusLog.logDel(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(),
                    task.getCType(), task.getLength(), destinationInfo.getAuthUser(), status, location);
            markFailWithRetry(task);
        }
    }

    /**
     * Delivery failed by reason of an exception.
     */
    public void reportException(DeliveryTask task, Exception exception) {
        StatusLog.logDel(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(),
                task.getCType(), task.getLength(), destinationInfo.getAuthUser(), -1, exception.toString());
        deliveryQueueHelper.handleUnreachable(destinationInfo);
        markFailWithRetry(task);
    }

    /**
     * Get the feed ID for a subscription.
     *
     * @param subid The subscription ID
     * @return The feed ID
     */
    public String getFeedId(String subid) {
        return (deliveryQueueHelper.getFeedId(subid));
    }

    /**
     * Get the URL to deliver a message to given the file ID.
     */
    public String getDestURL(String fileid) {
        return (deliveryQueueHelper.getDestURL(destinationInfo, fileid));
    }

    /**
     * Deliver files until there's a failure or there are no more.
     * files to deliver
     */
    public void run() {
        DeliveryTask task;
        long endtime = System.currentTimeMillis() + deliveryQueueHelper.getFairTimeLimit();
        int filestogo = deliveryQueueHelper.getFairFileLimit();
        while ((task = getNext()) != null) {
            logger.info("Processing file: " + task.getPublishId());
            task.run();
            if (--filestogo <= 0 || System.currentTimeMillis() > endtime) {
                break;
            }
        }
    }

    /**
     * Is there no work to do for this queue right now?.
     */
    synchronized boolean isSkipSet() {
        return (peekNext() == null);
    }

    /**
     * Reset the retry timer.
     */
    public void resetQueue() {
        resumetime = System.currentTimeMillis();
    }

    /**
     * Get task if in queue and mark as success.
     */
    public boolean markTaskSuccess(String pubId) {
        DeliveryTask task = working.get(pubId);
        if (task != null) {
            markSuccess(task);
            return true;
        }
        task = retry.get(pubId);
        if (task != null) {
            retry.remove(pubId);
            task.clean();
            resetQueue();
            failduration = 0;
            return true;
        }
        return false;
    }

    private void scanForNextTask(String[] files) {
        for (String fname : files) {
            String pubId = getPubId(fname);
            if (pubId == null) {
                continue;
            }
            DeliveryTask dt = retry.get(pubId);
            if (dt == null) {
                dt = new DeliveryTask(this, pubId);
            }
            todoList.add(dt);
        }
    }

    @Nullable
    private DeliveryTask getDeliveryTask(long mindate) {
        if (todoindex < todoList.size()) {
            DeliveryTask dt = todoList.get(todoindex);
            if (dt.isCleaned()) {
                todoindex++;
            }
            if (destinationInfo.isPrivilegedSubscriber() && dt.getResumeTime() > System.currentTimeMillis()) {
                retry.put(dt.getPublishId(), dt);
                todoindex++;
            }
            if (dt.getDate() >= mindate) {
                return (dt);
            }
            todoindex++;
            reportExpiry(dt);
        }
        return null;
    }

    @Nullable
    private String getPubId(String fname) {
        if (!fname.endsWith(".M")) {
            return null;
        }
        String fname2 = fname.substring(0, fname.length() - 2);
        long pidtime = 0;
        int dot = fname2.indexOf('.');
        if (dot < 1) {
            return null;
        }
        try {
            pidtime = Long.parseLong(fname2.substring(0, dot));
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        if (pidtime < 1000000000000L) {
            return null;
        }
        if (working.get(fname2) != null) {
            return null;
        }
        return fname2;
    }
}