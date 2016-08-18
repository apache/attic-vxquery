#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

hostname

CCHOST=$1
CCPORT=$2
J_OPTS=$3

#Import cluster properties
MYDIR="$(dirname -- $0)"
. ${MYDIR}/../conf/cluster.properties

# Export JAVA_HOME
export JAVA_HOME=${JAVA_HOME}

# java opts added parameters in XML Cluster config
if [ ! -z "${J_OPTS}" ]
then
    JAVA_OPTS="${JAVA_OPTS} ${J_OPTS}"
    export JAVA_OPTS
fi

# java opts added parameters Server cluster.properties
if [ ! -z "${CCJAVA_OPTS}" ]
then
    JAVA_OPTS="${JAVA_OPTS} ${CCJAVA_OPTS}"
    export JAVA_OPTS
fi

VXQUERY_HOME=`pwd`

# logs dir
mkdir -p ${CCLOGS_DIR}

# Set up the options for the cc.
CC_OPTIONS=" -client-net-ip-address ${CCHOST} -cluster-net-ip-address ${CCHOST} "
[ "${CCPORT}" ] &&

    CC_OPTIONS=" ${CC_OPTIONS} -client-net-port ${CCPORT} "


[ "${CCOPTS}" ] &&

    CC_OPTIONS=" ${CC_OPTIONS} ${CCOPTS}"


echo "${JAVA_OPTS}" &> ${CCLOGS_DIR}/cc.log

# Launch hyracks cc script without toplogy
${VXQUERY_HOME}/vxquery-server/target/appassembler/bin/vxquerycc ${CC_OPTIONS} &> ${CCLOGS_DIR}/cc_$(date +%Y%m%d%H%M).log &
