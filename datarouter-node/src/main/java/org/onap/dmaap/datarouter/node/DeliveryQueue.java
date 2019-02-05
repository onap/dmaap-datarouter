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


package org.onap.dmaap.datarouter.node;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mechanism for monitoring and controlling delivery of files to a destination.
 * <p>
 * The DeliveryQueue class maintains lists of DeliveryTasks for a single
 * destination (a subscription or another data router node) and assigns
 * delivery threads to try to deliver them.  It also maintains a delivery
 * status that causes it to back off on delivery attempts after a failure.
 * <p>
 * If the most recent delivery result was a failure, then no more attempts
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
 * <p>
 * The queue maintains 3 collections of files to deliver: A todo list of
 * files that will be attempted, a working set of files that are being
 * attempted, and a retry set of files that were attempted and failed.
 * Whenever the todo list is empty and needs to be refilled, a scan of the
 * spool directory is made and the file names sorted.  Any files in the working set are ignored.
 * If a DeliveryTask for the file is in the retry set, then that delivery
 * task is placed on the todo list.  Otherwise, a new DeliveryTask for the
 * file is created and placed on the todo list.
 * If, when a DeliveryTask is about to be removed from the todo list, its
 * age exceeds DeliveryQueueHelper.getExpirationTimer(), then it is instead
 * marked as expired.
 * <p>
 * A delivery queue also maintains a skip flag.  This flag is true if the
 * failure timer is active or if no files are found in a directory scan.
 */
public class DeliveryQueue implements Runnable, DeliveryTaskHelper {
    private DeliveryQueueHelper dqh;
    private DestInfo di;
    private ConcurrentHashMap<String, DeliveryTask> working = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, DeliveryTask> retry = new ConcurrentHashMap<>();
    private int todoindex;
    private boolean failed;
    private long failduration;
    private long resumetime;
    File dir;
    private ArrayList<DeliveryTask> todo = new ArrayList<>();

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
            for (int i = todoindex; i < todo.size(); i++) {
                DeliveryTask xdt = todo.get(i);
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
        StatusLog.logExp(dt.getPublishId(), dt.getFeedId(), dt.getSubId(), dt.getURL(), dt.getMethod(), dt.getCType(), dt.getLength(), "diskFull", dt.getAttempts());
        dt.clean();
        return (dt.getLength());
    }

    /**
     * Mark that a delivery task has succeeded.
     */
    public synchronized void markSuccess(DeliveryTask task) {
        working.remove(task.getPublishId());
        task.clean();
        failed = false;
        failduration = 0;
    }

    /**
     * Mark that a delivery task has expired.
     */
    public synchronized void markExpired(DeliveryTask task) {
        task.clean();
    }

    /**
     * Mark that a delivery task has failed permanently.
     */
    public synchronized void markFailNoRetry(DeliveryTask task) {
        working.remove(task.getPublishId());
        task.clean();
        failed = false;
        failduration = 0;
    }

    private void fdupdate() {
        if (!failed) {
            failed = true;
            if (failduration == 0) {
                failduration = dqh.getInitFailureTimer();
            }
            resumetime = System.currentTimeMillis() + failduration;
            long maxdur = dqh.getMaxFailureTimer();
            failduration = (long) (failduration * dqh.getFailureBackoff());
            if (failduration > maxdur) {
                failduration = maxdur;
            }
        }
    }

    /**
     * Mark that a delivery task has been redirected.
     */
    public synchronized void markRedirect(DeliveryTask task) {
        working.remove(task.getPublishId());
        retry.put(task.getPublishId(), task);
    }

    /**
     * Mark that a delivery task has temporarily failed.
     */
    public synchronized void markFailWithRetry(DeliveryTask task) {
        working.remove(task.getPublishId());
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
        long mindate = now - dqh.getExpirationTimer();
        if (failed) {
            if (now > resumetime) {
                failed = false;
            } else {
                return (null);
            }
        }
        while (true) {
            if (todoindex >= todo.size()) {
                todoindex = 0;
                todo = new ArrayList<>();
                String[] files = dir.list();
                Arrays.sort(files);
                for (String fname : files) {
                    if (!fname.endsWith(".M")) {
                        continue;
                    }
                    String fname2 = fname.substring(0, fname.length() - 2);
                    long pidtime = 0;
                    int dot = fname2.indexOf('.');
                    if (dot < 1) {
                        continue;
                    }
                    try {
                        pidtime = Long.parseLong(fname2.substring(0, dot));
                    } catch (Exception e) {
                    }
                    if (pidtime < 1000000000000L) {
                        continue;
                    }
                    if (working.get(fname2) != null) {
                        continue;
                    }
                    DeliveryTask dt = retry.get(fname2);
                    if (dt == null) {
                        dt = new DeliveryTask(this, fname2);
                    }
                    todo.add(dt);
                }
                retry = new ConcurrentHashMap<>();
            }
            if (todoindex < todo.size()) {
                DeliveryTask dt = todo.get(todoindex);
                if (dt.isCleaned()) {
                    todoindex++;
                    continue;
                }
                if (dt.getDate() >= mindate) {
                    return (dt);
                }
                todoindex++;
                reportExpiry(dt);
                continue;
            }
            return (null);
        }
    }

    /**
     * Create a delivery queue for a given destination info
     */
    public DeliveryQueue(DeliveryQueueHelper dqh, DestInfo di) {
        this.dqh = dqh;
        this.di = di;
        dir = new File(di.getSpool());
        dir.mkdirs();
    }

    /**
     * Update the destination info for this delivery queue
     */
    public void config(DestInfo di) {
        this.di = di;
    }

    /**
     * Get the dest info
     */
    public DestInfo getDestInfo() {
        return (di);
    }

    /**
     * Get the config manager
     */
    public DeliveryQueueHelper getConfig() {
        return (dqh);
    }

    /**
     * Exceptional condition occurred during delivery
     */
    public void reportDeliveryExtra(DeliveryTask task, long sent) {
        StatusLog.logDelExtra(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getLength(), sent);
    }

    /**
     * Message too old to deliver
     */
    public void reportExpiry(DeliveryTask task) {
        StatusLog.logExp(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(), task.getCType(), task.getLength(), "retriesExhausted", task.getAttempts());
        markExpired(task);
    }

    /**
     * Completed a delivery attempt
     */
    public void reportStatus(DeliveryTask task, int status, String xpubid, String location) {
        if (status < 300) {
            StatusLog.logDel(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(), task.getCType(), task.getLength(), di.getAuthUser(), status, xpubid);
            markSuccess(task);
        } else if (status < 400 && dqh.isFollowRedirects()) {
            StatusLog.logDel(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(), task.getCType(), task.getLength(), di.getAuthUser(), status, location);
            if (dqh.handleRedirection(di, location, task.getFileId())) {
                markRedirect(task);
            } else {
                StatusLog.logExp(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(), task.getCType(), task.getLength(), "notRetryable", task.getAttempts());
                markFailNoRetry(task);
            }
        } else if (status < 500) {
            StatusLog.logDel(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(), task.getCType(), task.getLength(), di.getAuthUser(), status, location);
            StatusLog.logExp(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(), task.getCType(), task.getLength(), "notRetryable", task.getAttempts());
            markFailNoRetry(task);
        } else {
            StatusLog.logDel(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(), task.getCType(), task.getLength(), di.getAuthUser(), status, location);
            markFailWithRetry(task);
        }
    }

    /**
     * Delivery failed by reason of an exception
     */
    public void reportException(DeliveryTask task, Exception exception) {
        StatusLog.logDel(task.getPublishId(), task.getFeedId(), task.getSubId(), task.getURL(), task.getMethod(), task.getCType(), task.getLength(), di.getAuthUser(), -1, exception.toString());
        dqh.handleUnreachable(di);
        markFailWithRetry(task);
    }

    /**
     * Get the feed ID for a subscription
     *
     * @param subid The subscription ID
     * @return The feed ID
     */
    public String getFeedId(String subid) {
        return (dqh.getFeedId(subid));
    }

    /**
     * Get the URL to deliver a message to given the file ID
     */
    public String getDestURL(String fileid) {
        return (dqh.getDestURL(di, fileid));
    }

    /**
     * Deliver files until there's a failure or there are no more
     * files to deliver
     */
    public void run() {
        DeliveryTask t;
        long endtime = System.currentTimeMillis() + dqh.getFairTimeLimit();
        int filestogo = dqh.getFairFileLimit();
        while ((t = getNext()) != null) {
            t.run();
            if (--filestogo <= 0 || System.currentTimeMillis() > endtime) {
                break;
            }
        }
    }

    /**
     * Is there no work to do for this queue right now?
     */
    public synchronized boolean isSkipSet() {
        return (peekNext() == null);
    }

    /**
     * Reset the retry timer
     */
    public void resetQueue() {
        resumetime = System.currentTimeMillis();
    }
}
