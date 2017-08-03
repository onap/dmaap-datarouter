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


package com.att.research.datarouter.node;

import java.util.*;

/**
 *	Execute an operation no more frequently than a specified interval
 */

public abstract class RateLimitedOperation implements Runnable	{
	private boolean	marked;	// a timer task exists
	private boolean	executing;	// the operation is currently in progress
	private boolean remark;	// a request was made while the operation was in progress
	private Timer	timer;
	private long	last;	// when the last operation started
	private long	mininterval;
	/**
	 *	Create a rate limited operation
	 *	@param mininterval	The minimum number of milliseconds after the last execution starts before a new execution can begin
	 *	@param timer	The timer used to perform deferred executions
	 */
	public RateLimitedOperation(long mininterval, Timer timer) {
		this.timer = timer;
		this.mininterval = mininterval;
	}
	private class deferred extends TimerTask	{
		public void run() {
			execute();
		}
	}
	private synchronized void unmark() {
		marked = false;
	}
	private void execute() {
		unmark();
		request();
	}
	/**
	 *	Request that the operation be performed by this thread or at a later time by the timer
	 */
	public void request() {
		if (premark()) {
			return;
		}
		do {
			run();
		} while (demark());
	}
	private synchronized boolean premark() {
		if (executing) {
			// currently executing - wait until it finishes
			remark = true;
			return(true);
		}
		if (marked) {
			// timer currently running - will run when it expires
			return(true);
		}
		long now = System.currentTimeMillis();
		if (last + mininterval > now) {
			// too soon - schedule a timer
			marked = true;
			timer.schedule(new deferred(), last + mininterval - now);
			return(true);
		}
		last = now;
		executing = true;
		// start execution
		return(false);
	}
	private synchronized boolean demark() {
		executing = false;
		if (remark) {
			remark = false;
			return(!premark());
		}
		return(false);
	}
}
