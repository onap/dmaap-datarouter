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

import java.util.*;

/**
 * Manage a list of tasks to be executed when an event occurs.
 * This makes the following guarantees:
 * <ul>
 * <li>Tasks can be safely added and removed in the middle of a run.</li>
 * <li>No task will be returned more than once during a run.</li>
 * <li>No task will be returned when it is not, at that moment, in the list of tasks.</li>
 * <li>At the moment when next() returns null, all tasks on the list have been returned during the run.</li>
 * <li>Initially and once next() returns null during a run, next() will continue to return null until startRun() is called.
 * </ul>
 */
public class TaskList {
    private Iterator<Runnable> runlist;
    private HashSet<Runnable> tasks = new HashSet<Runnable>();
    private HashSet<Runnable> togo;
    private HashSet<Runnable> sofar;
    private HashSet<Runnable> added;
    private HashSet<Runnable> removed;

    /**
     * Construct a new TaskList
     */
    public TaskList() {
    }

    /**
     * Start executing the sequence of tasks.
     */
    public synchronized void startRun() {
        sofar = new HashSet<Runnable>();
        added = new HashSet<Runnable>();
        removed = new HashSet<Runnable>();
        togo = new HashSet<Runnable>(tasks);
        runlist = togo.iterator();
    }

    /**
     * Get the next task to execute
     */
    public synchronized Runnable next() {
        while (runlist != null) {
            if (runlist.hasNext()) {
                Runnable task = runlist.next();
                if (removed.contains(task)) {
                    continue;
                }
                if (sofar.contains(task)) {
                    continue;
                }
                sofar.add(task);
                return (task);
            }
            if (added.size() != 0) {
                togo = added;
                added = new HashSet<Runnable>();
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
    public synchronized void addTask(Runnable task) {
        if (runlist != null) {
            added.add(task);
            removed.remove(task);
        }
        tasks.add(task);
    }

    /**
     * Remove a task from the list of tasks to run whenever the event occurs.
     */
    public synchronized void removeTask(Runnable task) {
        if (runlist != null) {
            removed.add(task);
            added.remove(task);
        }
        tasks.remove(task);
    }
}
