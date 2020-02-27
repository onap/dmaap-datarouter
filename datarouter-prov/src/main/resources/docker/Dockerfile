#########
#  ============LICENSE_START====================================================
#  org.onap.dmaap
#  ===========================================================================
#  Copyright © 2017 AT&T Intellectual Property. All rights reserved.
#  Modifications Copyright (C) 2018 Nokia. All rights reserved.
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
FROM openjdk:8-jre-alpine

MAINTAINER DMAAP Team

COPY /opt /opt

RUN apk update && apk add curl && apk add vim

EXPOSE 8080 8443

WORKDIR /opt
RUN chmod 0700 startup.sh

ENTRYPOINT ["sh", "startup.sh"]

RUN addgroup -S -g 1001 onap \
    && adduser -S -u 1000 dradmin -G onap \
    && chown -R dradmin:onap /opt/ /var/

USER dradmin

HEALTHCHECK CMD curl --fail http://localhost:8443/ || exit 1
