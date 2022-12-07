#!/bin/bash -x
#
# Copyright 2019 © Samsung Electronics Co., Ltd.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# This script installs common libraries required by CSIT tests
#

if [ -z "$WORKSPACE" ]; then
    # shellcheck disable=SC2155
    export WORKSPACE=`git rev-parse --show-toplevel`
fi

# shellcheck disable=SC2034
TESTPLANDIR=${WORKSPACE}/${TESTPLAN}

# Assume that if ROBOT3_VENV is set and virtualenv with system site packages can be activated,
# and install-robotframework.sh has already been executed

if [ -f ${WORKSPACE}/env.properties ]; then
    source ${WORKSPACE}/env.properties
fi
if [ -f ${ROBOT3_VENV}/bin/activate ]; then
    source ${ROBOT3_VENV}/bin/activate
else
    rm -f ${WORKSPACE}/env.properties
    source ${WORKSPACE}/install-robotframework.sh
fi

pip install --upgrade --extra-index-url="https://nexus3.onap.org/repository/PyPi.staging/simple" 'robotframework-onap==7.0.2.*' --pre
pip freeze
