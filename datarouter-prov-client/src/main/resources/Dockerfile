#  ============LICENSE_START====================================================
#  Copyright (C) 2023 J. F. Lucas.  All rights reserved.
#  ===========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END====================================================
#
FROM alpine:3.18.0

MAINTAINER DMAAP Team

COPY /opt /opt

WORKDIR /opt/app/drprov-client

# Install curl, jq, and gettext
RUN apk add --no-cache curl jq gettext

RUN chmod +x /opt/app/drprov-client/bin/*

RUN addgroup -S -g 1001 onap \
    && adduser -S -u 1000 onap -G onap \
    && chown -R onap:onap /opt/

USER onap

ENTRYPOINT ["sh", "/opt/app/drprov-client/bin/drprov-client.sh" ]