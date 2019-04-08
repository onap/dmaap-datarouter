/**
 * -
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */
package org.onap.dmaap.datarouter.provisioning.utils;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.onap.aaf.cadi.PropAccess;
import org.onap.aaf.cadi.filter.CadiFilter;
import org.onap.dmaap.datarouter.provisioning.BaseServlet;
import org.onap.dmaap.datarouter.provisioning.beans.EventLogRecord;
import org.onap.dmaap.datarouter.provisioning.beans.Feed;
import org.onap.dmaap.datarouter.provisioning.beans.Subscription;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class DRProvCadiFilter extends CadiFilter {
    protected static EELFLogger eventlogger = EELFManager.getInstance().getLogger("EventLog");
    protected static EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");
    private String aafInstance = "";

    public DRProvCadiFilter(boolean init, PropAccess access) throws ServletException {
        super(init, access);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        EventLogRecord elr = new EventLogRecord(httpRequest);
        String excludeAAF = httpRequest.getHeader(BaseServlet.EXCLUDE_AAF_HEADER);//send this param value as true, if want to add legacy feed/subscriber in AAF env

        String pathUrl = httpRequest.getServletPath();
        if (!(pathUrl.contains("internal") ||
                pathUrl.contains("sublog") ||
                pathUrl.contains("feedlog") ||
                pathUrl.contains("statistics") ||
                pathUrl.contains("publish") ||
                pathUrl.contains("group"))) {

            String method = httpRequest.getMethod().toUpperCase();
            if (!(method.equals("POST"))) { // if request method is PUT method (publish or Feed update) Needs to check for DELETE
                if (method.equals("PUT") || method.equals("DELETE")) {
                    if ((pathUrl.contains("subs"))) {//edit subscriber
                        int subId = BaseServlet.getIdFromPath(httpRequest);
                        if (subId <= 0) {
                            String message = String.format("Invalid request URI - %s", httpRequest.getPathInfo());
                            elr.setMessage(message);
                            elr.setResult(HttpServletResponse.SC_NOT_FOUND);
                            eventlogger.error(elr.toString());
                            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, message);
                            return;
                        }
                        if (isAAFSubscriber(subId)) {//edit AAF Subscriber
                            String message = String.format("DRProvCadiFilter - Edit AAF Subscriber : %d : AAF Instance - %s", subId, aafInstance);
                            elr.setMessage(message);
                            eventlogger.info(elr.toString());
                            //request.setAttribute("aafInstance", aafInstance);// no need to set it in request since it is taken care in respective servlets
                            super.doFilter(request, response, chain);

                        } else {//Edit or publish legacy Subscriber
                            String message = "DRProvCadiFilter - Edit/Publish Legacy Subscriber :" + subId;
                            elr.setMessage(message);
                            eventlogger.info(elr.toString());
                            chain.doFilter(request, response);
                        }

                    } else {//edit or publish Feed
                        int feedId = BaseServlet.getIdFromPath(httpRequest);
                        if (feedId <= 0) {
                            String message = "Invalid request URI - " + httpRequest.getPathInfo();
                            elr.setMessage(message);
                            elr.setResult(HttpServletResponse.SC_NOT_FOUND);
                            eventlogger.error(elr.toString());
                            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, message);
                            return;
                        }

                        if (isAAFFeed(feedId)) {//edit AAF Feed
                            String message = "DRProvCadiFilter - Edit AAF Feed:" + feedId + ":" + "AAF Instance -" + aafInstance;
                            elr.setMessage(message);
                            eventlogger.info(elr.toString());
                            super.doFilter(request, response, chain);

                        } else {//Edit or publish legacy Feed
                            String message = "DRProvCadiFilter - Edit/Publish Legacy Feed:" + feedId;
                            elr.setMessage(message);
                            eventlogger.info(elr.toString());
                            chain.doFilter(request, response);
                        }
                    }
                } else {// in all other cases defaults to legacy behavior
                    String message = "DRProvCadiFilter - Default Legacy Feed/Subscriber URI -:" + httpRequest.getPathInfo();
                    elr.setMessage(message);
                    eventlogger.info(elr.toString());
                    chain.doFilter(request, response);
                }
            } else {
                //check to add legacy/AAF subscriber
                if ((pathUrl.contains("subscribe"))) {//add subscriber
                    int feedId = BaseServlet.getIdFromPath(httpRequest);
                    if (feedId <= 0) {
                        String message = "Invalid request URI - " + httpRequest.getPathInfo();
                        elr.setMessage(message);
                        elr.setResult(HttpServletResponse.SC_NOT_FOUND);
                        eventlogger.error(elr.toString());
                        httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, message);
                        return;
                    }
                    if (isAAFFeed(feedId)) {//check if AAF Feed or legacy to add new subscriber
                        if (excludeAAF == null) {
                            String message = "DRProvCadiFilter -Invalid request Header Parmeter " + BaseServlet.EXCLUDE_AAF_HEADER + " = " + httpRequest.getHeader(BaseServlet.EXCLUDE_AAF_HEADER);
                            elr.setMessage(message);
                            elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                            eventlogger.error(elr.toString());
                            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
                            return;
                        }
                        if (excludeAAF.equalsIgnoreCase("true")) {//Check to add legacy subscriber to AAF Feed
                            String message = "DRProvCadiFilter - add legacy subscriber to AAF Feed, FeedID:" + feedId;
                            elr.setMessage(message);
                            eventlogger.info(elr.toString());
                            chain.doFilter(request, response);
                        } else {
                            String message = "DRProvCadiFilter - Add AAF subscriber to AAF Feed, FeedID:" + feedId + ":" + "AAF Instance -" + aafInstance;
                            elr.setMessage(message);
                            eventlogger.info(elr.toString());
                            super.doFilter(request, response, chain);
                        }
                    } else {//Add legacy susbcriber to legacy Feed
                        String message = "DRProvCadiFilter - add legacy subscriber to legacy Feed:" + feedId;
                        elr.setMessage(message);
                        eventlogger.info(elr.toString());
                        chain.doFilter(request, response);
                    }
                } else {//add AAF feed
                    if (excludeAAF == null) {
                        String message = "DRProvCadiFilter -Invalid request Header Parmeter " + BaseServlet.EXCLUDE_AAF_HEADER + " = " + httpRequest.getHeader(BaseServlet.EXCLUDE_AAF_HEADER);
                        elr.setMessage(message);
                        elr.setResult(HttpServletResponse.SC_BAD_REQUEST);
                        eventlogger.error(elr.toString());
                        httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
                        return;
                    }
                    if (excludeAAF.equalsIgnoreCase("true")) {//add legacy feed
                        String message = "DRProvCadiFilter - Create new legacy Feed : EXCLUDE_AAF = " + excludeAAF;
                        elr.setMessage(message);
                        eventlogger.info(elr.toString());
                        chain.doFilter(request, response);
                    } else {//add AAF Feed
                        String message = "DRProvCadiFilter - Create new AAF Feed : EXCLUDE_AAF = " + excludeAAF;
                        elr.setMessage(message);
                        eventlogger.info(elr.toString());
                        super.doFilter(request, response, chain);
                    }
                }
            }
        } else {
            //All other requests default to (Non CADI) legacy
            chain.doFilter(request, response);
        }
    }

    /**
     * Check if it is AAF feed OR existing feed.
     *
     * @param feedId the Feed ID
     * @return true if it is valid
     */
    @SuppressWarnings("resource")
    private boolean isAAFFeed(int feedId) {
        try {
            Feed feed = Feed.getFeedById(feedId);
            if (feed != null) {
                if (!((feed.getAafInstance().equalsIgnoreCase("legacy")) || feed.getAafInstance() == null || feed.getAafInstance().equals(""))) { //also apply null check and empty check too
                    aafInstance = feed.getAafInstance();
                    String message = "DRProvCadiFilter.isAAFFeed: aafInstance-:" + aafInstance + "; feedId:- " + feedId;
                    intlogger.debug(message);
                    return true;
                } else {
                    return false;
                }
            } else {
                String message = "DRProvCadiFilter.isAAFFeed; Feed does not exist FeedID:-" + feedId;
                intlogger.debug(message);
            }

        } catch (Exception e) {
            intlogger.error("PROV0073 DRProvCadiFilter.isAAFFeed: ", e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * Check if it is AAF sub OR existing sub.
     *
     * @param subId the Sub ID
     * @return true if it is valid
     */
    @SuppressWarnings("resource")
    private boolean isAAFSubscriber(int subId) {
        try {
            Subscription subscriber = Subscription.getSubscriptionById(subId);
            if (subscriber != null) {
                if (!((subscriber.getAafInstance().equalsIgnoreCase("legacy")) || subscriber.getAafInstance() == null || subscriber.getAafInstance().equals(""))) { //also apply null check and empty check too
                    aafInstance = subscriber.getAafInstance();
                    String message = "DRProvCadiFilter.isAAFSubscriber: aafInstance-:" + aafInstance + "; subId:- " + subId;
                    intlogger.debug(message);
                    return true;
                } else {
                    return false;
                }
            } else {
                String message = "DRProvCadiFilter.isAAFSubscriber; Subscriber does not exist subId:-" + subId;
                intlogger.debug(message);
            }
        } catch (Exception e) {
            intlogger.error("PROV0073 DRProvCadiFilter.isAAFSubscriber: ", e.getMessage());
            return false;
        }
        return false;
    }

}
