#!/bin/bash -x
#
# Copyright 2020-2021 © Samsung Electronics Co., Ltd.
# Modifications copyright (C) 2021 Nordix Foundation..
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

# shellcheck disable=SC2155
export WORKSPACE=$(git rev-parse --show-toplevel)/csit

rm -rf "${WORKSPACE}"/archives
mkdir -p "${WORKSPACE}"/archives
# shellcheck disable=SC2164
cd "${WORKSPACE}"

./run-csit.sh plans/dmaap-datarouter/dr-suite



