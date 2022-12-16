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

import java.util.HashSet;
import java.util.Iterator;

/**
 * Manage a list of tasks to be executed when an event occurs. This makes the following guarantees:
 * <ul>
 * <li>Tasks can be safely added and removed in the middle of a run.</li>
 * <li>No task will be returned more than once during a run.</li>
 * <li>No task will be returned when it is not, at that moment, in the list of tasks.</li>
 * <li>At the moment when next() returns null, all tasks on the list have been returned during the run.</li>
 * <li>Initially and once next() returns null during a run, next() will continue to return null until startRun() is
 * called.
 * </ul>
 */
class TaskList {

    private Iterator<Runnable> runlist;
    private final HashSet<Runnable> tasks = new HashSet<>();
    private HashSet<Runnable> togo;
    private HashSet<Runnable> sofar;
    private HashSet<Runnable> added;
    private HashSet<Runnable> removed;

    /**
     * Start executing the sequence of tasks.
     */
    synchronized void startRun() {
        sofar = new HashSet<>();
        added = new HashSet<>();
        removed = new HashSet<>();
        togo = new HashSet<>(tasks);
        runlist = togo.iterator();
    }

    /**
     * Get the next task to execute.
     */
    synchronized Runnable next() {
        while (runlist != null) {
            if (runlist.hasNext()) {
                Runnable task = runlist.next();
                if (addTaskToSoFar(task)) {
                    return task;
                }
            }
            if (!added.isEmpty()) {
                togo = added;
                added = new HashSet<>();
                removed.clear();
                runlist = togo.iterator();
                continue;
            }
            togo = null;
            added = null;
            removed = null;
            sofar = null;
            runlist = null;
        }
        return (null);
    }

    /**
     * Add a task to the list of tasks to run whenever the event occurs.
     */
    synchronized void addTask(Runnable task) {
        if (runlist != null) {
            added.add(task);
            removed.remove(task);
        }
        tasks.add(task);
    }

    /**
     * Remove a task from the list of tasks to run whenever the event occurs.
     */
    synchronized void removeTask(Runnable task) {
        if (runlist != null) {
            removed.add(task);
            added.remove(task);
        }
        tasks.remove(task);
    }

    private boolean addTaskToSoFar(Runnable task) {
        if (removed.contains(task)) {
            return false;
        }
        if (sofar.contains(task)) {
            return false;
        }
        sofar.add(task);
        return true;
    }
}
