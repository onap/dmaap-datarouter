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

    private String name;
    private String spool;
    private String subid;
    private String logdata;
    private String url;
    private String authuser;
    private String authentication;
    private boolean metaonly;
    private boolean use100;
    private boolean privilegedSubscriber;
    private boolean followRedirects;
    private boolean decompress;

    public String getName() {
        return name;
    }

    public DestInfoBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public String getSpool() {
        return spool;
    }

    public DestInfoBuilder setSpool(String spool) {
        this.spool = spool;
        return this;
    }

    public String getSubid() {
        return subid;
    }

    public DestInfoBuilder setSubid(String subid) {
        this.subid = subid;
        return this;
    }

    String getLogdata() {
        return logdata;
    }

    DestInfoBuilder setLogdata(String logdata) {
        this.logdata = logdata;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public DestInfoBuilder setUrl(String url) {
        this.url = url;
        return this;
    }

    String getAuthuser() {
        return authuser;
    }

    DestInfoBuilder setAuthuser(String authuser) {
        this.authuser = authuser;
        return this;
    }

    String getAuthentication() {
        return authentication;
    }

    DestInfoBuilder setAuthentication(String authentication) {
        this.authentication = authentication;
        return this;
    }

    boolean isMetaonly() {
        return metaonly;
    }

    DestInfoBuilder setMetaonly(boolean metaonly) {
        this.metaonly = metaonly;
        return this;
    }

    boolean isUse100() {
        return use100;
    }

    DestInfoBuilder setUse100(boolean use100) {
        this.use100 = use100;
        return this;
    }

    boolean isPrivilegedSubscriber() {
        return privilegedSubscriber;
    }

    DestInfoBuilder setPrivilegedSubscriber(boolean privilegedSubscriber) {
        this.privilegedSubscriber = privilegedSubscriber;
        return this;
    }

    boolean isFollowRedirects() {
        return followRedirects;
    }

    DestInfoBuilder setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    boolean isDecompress() {
        return decompress;
    }

    DestInfoBuilder setDecompress(boolean decompress) {
        this.decompress = decompress;
        return this;
    }

    DestInfo createDestInfo() {
        return new DestInfo(this);
    }
}