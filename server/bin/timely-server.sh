#!/bin/bash

if [[ `uname` == "Darwin" ]]; then
        THIS_SCRIPT=`python -c 'import os,sys; print os.path.realpath(sys.argv[1])' $0`
        TCNATIVE_SUFFIX="jnilib"
else
        THIS_SCRIPT=`readlink -f $0`
        TCNATIVE_SUFFIX="so"
fi

THIS_DIR="${THIS_SCRIPT%/*}"
NATIVE_DIR="${THIS_DIR}/META-INF/native"
BASE_DIR=${THIS_DIR}/..
TMP_DIR="${BASE_DIR}/tmp"
CONF_DIR="${BASE_DIR}/conf"
LIB_DIR="${BASE_DIR}/lib"
NUM_SERVER_THREADS=4

if [[ -e ${TMP_DIR} ]]; then
  rm -rf ${TMP_DIR}
fi
mkdir ${TMP_DIR}

if [[ -e ${NATIVE_DIR} ]]; then
  rm -rf ${NATIVE_DIR}
fi
mkdir -p ${NATIVE_DIR}

pushd ${BASE_DIR}/bin
$JAVA_HOME/bin/jar xf ${LIB_DIR}/netty-tcnative*.jar META-INF/native/libnetty_tcnative.${TCNATIVE_SUFFIX}
$JAVA_HOME/bin/jar xf ${LIB_DIR}/netty-transport-native-epoll*.jar META-INF/native/libnetty_transport_native_epoll_x86_64.${TCNATIVE_SUFFIX}
popd

export CLASSPATH="${CONF_DIR}:${LIB_DIR}/*"
JVM_ARGS="-Xmx256m -Xms256m -Dio.netty.eventLoopThreads=${NUM_SERVER_THREADS}"
JVM_ARGS="${JVM_ARGS} -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
JVM_ARGS="${JVM_ARGS} -Djava.library.path=${NATIVE_DIR}"
JVM_ARGS="${JVM_ARGS} -Djava.library.path=${NATIVE_DIR}/libnetty-${TCNATIVE_SUFFIX}"

echo "$JAVA_HOME/bin/java ${JVM_ARGS} timely.Server --spring.config.name=timely"
$JAVA_HOME/bin/java ${JVM_ARGS} timely.Server --spring.config.name=timely

