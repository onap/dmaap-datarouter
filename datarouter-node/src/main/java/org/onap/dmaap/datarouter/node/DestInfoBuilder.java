/*-
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

public class DestInfoBuilder {

    private String destInfoName;
    private String destInfoSpool;
    private String destInfoSubId;
    private String destInfoLogData;
    private String destInfoUrl;
    private String destInfoAuthUser;
    private String destInfoAuthentication;
    private boolean destInfoMetaOnly;
    private boolean destInfoUse100;
    private boolean destInfoPrivilegedSubscriber;
    private boolean destInfoFollowRedirects;
    private boolean destInfoDecompress;

    public String getName() {
        return destInfoName;
    }

    public DestInfoBuilder setName(String name) {
        this.destInfoName = name;
        return this;
    }

    public String getSpool() {
        return destInfoSpool;
    }

    public DestInfoBuilder setSpool(String spool) {
        this.destInfoSpool = spool;
        return this;
    }

    public String getSubid() {
        return destInfoSubId;
    }

    public DestInfoBuilder setSubid(String subid) {
        this.destInfoSubId = subid;
        return this;
    }

    String getLogdata() {
        return destInfoLogData;
    }

    DestInfoBuilder setLogdata(String logdata) {
        this.destInfoLogData = logdata;
        return this;
    }

    public String getUrl() {
        return destInfoUrl;
    }

    public DestInfoBuilder setUrl(String url) {
        this.destInfoUrl = url;
        return this;
    }

    String getAuthuser() {
        return destInfoAuthUser;
    }

    DestInfoBuilder setAuthuser(String authuser) {
        this.destInfoAuthUser = authuser;
        return this;
    }

    String getAuthentication() {
        return destInfoAuthentication;
    }

    DestInfoBuilder setAuthentication(String authentication) {
        this.destInfoAuthentication = authentication;
        return this;
    }

    boolean isMetaonly() {
        return destInfoMetaOnly;
    }

    DestInfoBuilder setMetaonly(boolean metaonly) {
        this.destInfoMetaOnly = metaonly;
        return this;
    }

    boolean isUse100() {
        return destInfoUse100;
    }

    DestInfoBuilder setUse100(boolean use100) {
        this.destInfoUse100 = use100;
        return this;
    }

    boolean isPrivilegedSubscriber() {
        return destInfoPrivilegedSubscriber;
    }

    DestInfoBuilder setPrivilegedSubscriber(boolean privilegedSubscriber) {
        this.destInfoPrivilegedSubscriber = privilegedSubscriber;
        return this;
    }

    boolean isFollowRedirects() {
        return destInfoFollowRedirects;
    }

    DestInfoBuilder setFollowRedirects(boolean followRedirects) {
        this.destInfoFollowRedirects = followRedirects;
        return this;
    }

    boolean isDecompress() {
        return destInfoDecompress;
    }

    DestInfoBuilder setDecompress(boolean decompress) {
        this.destInfoDecompress = decompress;
        return this;
    }

    DestInfo createDestInfo() {
        return new DestInfo(this);
    }
}