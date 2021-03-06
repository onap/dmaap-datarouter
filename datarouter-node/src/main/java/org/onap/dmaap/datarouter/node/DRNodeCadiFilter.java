/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dmaap.datarouter.node;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.onap.aaf.cadi.PropAccess;
import org.onap.aaf.cadi.filter.CadiFilter;


public class DRNodeCadiFilter extends CadiFilter {

    private static EELFLogger logger = EELFManager.getInstance().getLogger(DRNodeCadiFilter.class);

    DRNodeCadiFilter(boolean init, PropAccess access) throws ServletException {
        super(init, access);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getPathInfo();
        if (!(path.startsWith("/internal"))) {
            if (!("POST".equalsIgnoreCase(httpRequest.getMethod()))) {
                if ("DELETE".equalsIgnoreCase(httpRequest.getMethod()) && path.startsWith("/delete")) {
                    chain.doFilter(request, response);
                } else {
                    doFilterWithFeedId(request, response, chain);
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private String getFeedId(ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String fileid = req.getPathInfo();
        if (fileid == null) {
            logger.error("NODE0105 Rejecting bad URI for PUT " + req.getPathInfo() + " from " + req.getRemoteAddr());
            try {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Invalid request URI.  Expecting <feed-publishing-url>/<fileid>.");
            } catch (IOException e) {
                logger.error("NODE0541 DRNodeCadiFilter.getFeedId: ", e);
            }
            return null;
        }
        String feedid = "";

        if (fileid.startsWith("/publish/")) {
            fileid = fileid.substring(9);
            int index = fileid.indexOf('/');
            if (index == -1 || index == fileid.length() - 1) {
                logger.error("NODE0105 Rejecting bad URI for PUT (publish) of " + req.getPathInfo() + " from " + req
                        .getRemoteAddr());
                try {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                            "Invalid request URI.  Expecting <feed-publishing-url>/<fileid>.  "
                                    + "Possible missing fileid.");
                } catch (IOException e) {
                    logger.error("NODE0542 DRNodeCadiFilter.getFeedId: ", e);
                }
                return null;
            }
            feedid = fileid.substring(0, index);
        }
        return feedid;
    }

    private void doFilterWithFeedId(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String feedId = getFeedId(request, response);
        String aafDbInstance = NodeConfigManager.getInstance().getAafInstance(feedId);
        if (aafDbInstance != null && !"".equals(aafDbInstance) && !"legacy".equalsIgnoreCase(aafDbInstance)) {
            logger.info("DRNodeCadiFilter - doFilter: FeedId - " + feedId + ":" + "AAF Instance -" + aafDbInstance);
            super.doFilter(request, response, chain);
        } else {
            logger.info("DRNodeCadiFilter - doFilter: FeedId - " + feedId + ":" + "Legacy Feed");
            chain.doFilter(request, response);
        }
    }
}
