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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.onap.dmaap.datarouter.provisioning.beans.Parameters;

/**
 * This filter checks /publish requests to the provisioning server to allow ill-behaved publishers to be throttled.
 * It is configured via the provisioning parameter THROTTLE_FILTER.
 * The THROTTLE_FILTER provisioning parameter can have these values:
 * <table>
 * <tr><td>(no value)</td><td>filter disabled</td></tr>
 * <tr><td>off</td><td>filter disabled</td></tr>
 * <tr><td>N[,M[,action]]</td><td>set N, M, and action (used in the algorithm below).
 * Action is <i>drop</i> or <i>throttle</i>.
 * If M is missing, it defaults to 5 minutes.
 * If the action is missing, it defaults to <i>drop</i>.
 * </td></tr>
 * </table>
 *
 * <p>* The <i>action</i> is triggered iff:
 * <ol>
 * <li>the filter is enabled, and</li>
 * <li>N /publish requests come to the provisioning server in M minutes
 * <ol>
 * <li>from the same IP address</li>
 * <li>for the same feed</li>
 * <li>lacking the <i>Expect: 100-continue</i> header</li>
 * </ol>
 * </li>
 * </ol>
 * The action that can be performed (if triggered) are:
 * <ol>
 * <li><i>drop</i> - the connection is dropped immediately.</li>
 * <li><i>throttle</i> - [not supported] the connection is put into a low priority queue
 * with all other throttled connections.
 * These are then processed at a slower rate.  Note: this option does not work correctly, and is disabled.
 * The only action that is supported is <i>drop</i>.
 * </li>
 * </ol>
 *
 * @author Robert Eby
 * @version $Id: ThrottleFilter.java,v 1.2 2014/03/12 19:45:41 eby Exp $
 */

public class ThrottleFilter extends TimerTask implements Filter {
    private static final int DEFAULT_N = 10;
    private static final int DEFAULT_M = 5;
    private static final String THROTTLE_MARKER = "org.onap.dmaap.datarouter.provisioning.THROTTLE_MARKER";
    private static final String JETTY_REQUEST = "org.eclipse.jetty.server.Request";
    private static final long ONE_MINUTE = 60000L;
    private static final int ACTION_DROP = 0;
    private static final int ACTION_THROTTLE = 1;

    // Configuration
    private static boolean enabled = false;        // enabled or not
    private static int numRequests = 0;            // number of requests in M minutes
    private static int samplingPeriod = 0;            // sampling period
    private static int action = ACTION_DROP;    // action to take (throttle or drop)

    private static EELFLogger logger = EELFManager.getInstance().getLogger("InternalLog");
    private static Map<String, Counter> map = new HashMap<>();
    private static final Timer rolex = new Timer();

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        configure();
        rolex.scheduleAtFixedRate(this, 5 * 60000L, 5 * 60000L);    // Run once every 5 minutes to clean map
    }

    /**
     * Configure the throttle.  This should be called from BaseServlet.provisioningParametersChanged(),
     * to make sure it stays up to date.
     */
    public static void configure() {
        Parameters param = Parameters.getParameter(Parameters.THROTTLE_FILTER);
        if (param != null) {
            try {
                Class.forName(JETTY_REQUEST);
                String val = param.getValue();
                if (val != null && !"off".equals(val)) {
                    String[] pp = val.split(",");
                    if (pp != null) {
                        numRequests = (pp.length > 0) ? getInt(pp[0], DEFAULT_N) : DEFAULT_N;
                        samplingPeriod = (pp.length > 1) ? getInt(pp[1], DEFAULT_M) : DEFAULT_M;
                        action = (pp.length > 2 && pp[2] != null
                                          && "throttle".equalsIgnoreCase(pp[2])) ? ACTION_THROTTLE : ACTION_DROP;
                        enabled = true;
                        // ACTION_THROTTLE is not currently working, so is not supported
                        if (action == ACTION_THROTTLE) {
                            action = ACTION_DROP;
                            logger.info("Throttling is not currently supported; action changed to DROP");
                        }
                        logger.info("ThrottleFilter is ENABLED for /publish requests; N="
                                            + numRequests + ", M=" + samplingPeriod
                            + ", Action=" + action);
                        return;
                    }
                }
            } catch (ClassNotFoundException e) {
                logger.warn("Class " + JETTY_REQUEST + " is not available; this filter requires Jetty.", e);
            }
        }
        logger.info("ThrottleFilter is DISABLED for /publish requests.");
        enabled = false;
        map.clear();
    }

    private static int getInt(String str, int deflt) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException x) {
            return deflt;
        }
    }

    @Override
    public void destroy() {
        rolex.cancel();
        map.clear();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (enabled && action == ACTION_THROTTLE) {
            throttleFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        } else if (enabled) {
            dropFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Method to drop filter chain.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param chain FilterChain
     * @throws IOException input/output exception
     * @throws ServletException servle exception
     */
    public void dropFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        int rate = getRequestRate(request);
        if (rate >= numRequests) {
            // drop request - only works under Jetty
            String str = String.format("Dropping connection: %s %d bad connections in %d minutes",
                    getConnectionId(request), rate,
                samplingPeriod);
            logger.info(str);
            Request baseRequest = (request instanceof Request)
                    ? (Request) request
                    : HttpConnection.getCurrentConnection().getHttpChannel().getRequest();
            baseRequest.getHttpChannel().getEndPoint().close();
        } else {
            chain.doFilter(request, response);
        }
    }

    private void throttleFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // throttle request
        String id = getConnectionId(request);
        int rate = getRequestRate(request);
        Object results = request.getAttribute(THROTTLE_MARKER);
        if (rate >= numRequests && results == null) {
            String str = String.format("Throttling connection: %s %d bad connections in %d minutes",
                getConnectionId(request), rate, samplingPeriod);
            logger.info(str);
            Continuation continuation = ContinuationSupport.getContinuation(request);
            continuation.suspend();
            register(id, continuation);
            continuation.undispatch();
        } else {
            chain.doFilter(request, response);
            @SuppressWarnings("resource")
            InputStream is = request.getInputStream();
            byte[] bite = new byte[4096];
            int num = is.read(bite);
            while (num > 0) {
                num = is.read(bite);
            }
            resume(id);
        }
    }

    private Map<String, List<Continuation>> suspendedRequests = new HashMap<>();

    private void register(String id, Continuation continuation) {
        synchronized (suspendedRequests) {
            List<Continuation> list = suspendedRequests.get(id);
            if (list == null) {
                list = new ArrayList<>();
                suspendedRequests.put(id, list);
            }
            list.add(continuation);
        }
    }

    private void resume(String id) {
        synchronized (suspendedRequests) {
            List<Continuation> list = suspendedRequests.get(id);
            if (list != null) {
                // when the waited for event happens
                Continuation continuation = list.remove(0);
                continuation.setAttribute(ThrottleFilter.THROTTLE_MARKER, new Object());
                continuation.resume();
            }
        }
    }

    /**
     * Return a count of number of requests in the last M minutes, iff this is a "bad" request.
     * If the request has been resumed (if it contains the THROTTLE_MARKER) it is considered good.
     *
     * @param request the request
     * @return number of requests in the last M minutes, 0 means it is a "good" request
     */
    private int getRequestRate(HttpServletRequest request) {
        String expecthdr = request.getHeader("Expect");
        if (expecthdr != null && "100-continue".equalsIgnoreCase(expecthdr)) {
            return 0;
        }

        String key = getConnectionId(request);
        synchronized (map) {
            Counter cnt = map.get(key);
            if (cnt == null) {
                cnt = new Counter();
                map.put(key, cnt);
            }
            return cnt.getRequestRate();
        }
    }

    public class Counter {
        private List<Long> times = new ArrayList<>();    // a record of request times

        /**
         * Method to prune request rate.
         * @return times
         */
        public int prune() {
            try {
                long num = System.currentTimeMillis() - (samplingPeriod * ONE_MINUTE);
                long time = times.get(0);
                while (time < num) {
                    times.remove(0);
                    time = times.get(0);
                }
            } catch (IndexOutOfBoundsException e) {
                logger.trace("Exception: " + e.getMessage(), e);
            }
            return times.size();
        }

        public int getRequestRate() {
            times.add(System.currentTimeMillis());
            return prune();
        }
    }

    /**
     * Identify a connection by endpoint IP address, and feed ID.
     */
    private String getConnectionId(HttpServletRequest req) {
        return req.getRemoteAddr() + "/" + getFeedId(req);
    }

    private int getFeedId(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.length() < 2) {
            return -1;
        }
        path = path.substring(1);
        int ix = path.indexOf('/');
        if (ix < 0 || ix == path.length() - 1) {
            return -2;
        }
        try {
            return Integer.parseInt(path.substring(0, ix));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public void run() {
        // Once every 5 minutes, go through the map, and remove empty entrys
        for (Object s : map.keySet().toArray()) {
            synchronized (map) {
                Counter counter = map.get(s);
                if (counter.prune() <= 0) {
                    map.remove(s);
                }
            }
        }
    }
}
