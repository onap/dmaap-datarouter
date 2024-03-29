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

package org.onap.dmaap.datarouter.authz.impl;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.onap.dmaap.datarouter.authz.AuthorizationResponse;
import org.onap.dmaap.datarouter.authz.Authorizer;
import org.onap.dmaap.datarouter.authz.impl.AuthzResource.ResourceType;

/** Authorizer for the provisioning API for Data Router R1.
 *
 * @author J. F. Lucas
 *
 */
public class ProvAuthorizer implements Authorizer {

    private EELFLogger log;
    private ProvDataProvider provData;

    private static final String SUBJECT_HEADER = "X-DMAAP-DR-ON-BEHALF-OF";  // HTTP header carrying requester identity
    // HTTP header carrying requester identity  by group Rally : US708115
    private static final String SUBJECT_HEADER_GROUP = "X-DMAAP-DR-ON-BEHALF-OF-GROUP";

    /** Constructor. For the moment, do nothing special.  Make it a singleton?
     *
     */
    public ProvAuthorizer(ProvDataProvider provData) {
        this.provData = provData;
        this.log = EELFManager.getInstance().getLogger(this.getClass());
    }

    /**
     * Determine if the API request carried in the <code>request</code> parameter is permitted.
     *
     * @param request the HTTP request for which an authorization decision is needed
     * @return an object implementing the <code>AuthorizationResponse</code> interface.  This object includes the
     * permit/deny decision for the request and (after R1) supplemental information related to the response in the form
     * of advice and obligations.
     */
    @Override
    public AuthorizationResponse decide(HttpServletRequest request) {
        return this.decide(request, null);
    }

    /**
     * Determine if the API request carried in the <code>request</code> parameter,with additional attributes provided in
     * the <code>additionalAttrs</code> parameter, is permitted.   <code>additionalAttrs</code> isn't used in R1.
     *
     * @param request the HTTP request for which an authorization decision is needed
     * @param additionalAttrs additional attributes that the <code>Authorizer</code> can in making a decision
     * @return an object implementing the <code>AuthorizationResponse</code> interface.  This object includes the
     * permit/deny decision for the request and (after R1) supplemental information related to the response in the form
     * of advice and obligations.
     */
    @Override
    public AuthorizationResponse decide(HttpServletRequest request,
            Map<String, String> additionalAttrs) {
        log.trace("Entering decide()");
        boolean decision = false;
        // Extract interesting parts of the HTTP request
        String method = request.getMethod();
        AuthzResource resource = new AuthzResource(request.getRequestURI());
        String subject = (request.getHeader(SUBJECT_HEADER));
        String subjectgroup = (request.getHeader(SUBJECT_HEADER_GROUP));

        log.trace("Method: " + method + " -- Type: " + resource.getType() + " -- Id: " + resource.getId()
                          + " -- Subject: " + subject);
        // Choose authorization method based on the resource type
        ResourceType resourceType = resource.getType();
        if (resourceType != null) {
            switch (resourceType) {
                case FEEDS_COLLECTION:
                    decision = allowFeedsCollectionAccess(method);
                    break;
                case SUBS_COLLECTION:
                    decision = allowSubsCollectionAccess(method);
                    break;
                case FEED:
                    decision = allowFeedAccess(resource, method, subject, subjectgroup);
                    break;
                case SUB:
                    decision = allowSubAccess(resource, method, subject, subjectgroup);
                    break;
                default:
                    break;
            }
        }
        log.debug("Exit decide(): "  + method + "|" + resourceType + "|" + resource.getId() + "|"
                          + subject + " ==> " + decision);

        return new AuthRespImpl(decision);
    }

    private boolean allowFeedsCollectionAccess(String method) {
        // Allow GET or POST unconditionally
        return method != null && ("GET".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method));
    }

    private boolean allowSubsCollectionAccess(String method) {
        // Allow GET or POST unconditionally
        return method != null && ("GET".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method));
    }

    private boolean allowFeedAccess(AuthzResource resource, String method, String subject, String subjectgroup) {
        boolean decision = false;
        // Allow GET, PUT, or DELETE if requester (subject) is the owner (publisher) of the feed
        if ( method != null && ("GET".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                                        || "DELETE".equalsIgnoreCase(method))) {

            String owner = provData.getFeedOwner(resource.getId());
            decision = (owner != null) && owner.equals(subject);
            //Verifying by group Rally : US708115
            if (subjectgroup != null) {
                String feedOwner = provData.getGroupByFeedGroupId(subject, resource.getId());
                decision = (feedOwner != null) && feedOwner.equals(subjectgroup);
            }
        }
        return decision;
    }

    private boolean allowSubAccess(AuthzResource resource, String method, String subject, String subjectgroup) {
        boolean decision = false;

        // Allow GET, PUT, or DELETE if requester (subject) is the owner of the subscription (subscriber)
        if (method != null && ("GET".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                                       || "DELETE".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method))) {

            String owner = provData.getSubscriptionOwner(resource.getId());
            decision = (owner != null) && owner.equals(subject);

            //Verifying by group Rally : US708115
            if (subjectgroup != null) {
                String feedowner = provData.getGroupBySubGroupId(subject, resource.getId());
                decision = (feedowner != null) && feedowner.equals(subjectgroup);
            }
        }

        return decision;
    }

}
