#!/bin/bash
LIB=/opt/app/datartr/lib
ETC=/opt/app/datartr/etc
echo "this is LIB" $LIB
echo "this is ETC" $ETC
mkdir -p /opt/app/datartr/logs
mkdir -p /opt/app/datartr/spool
mkdir -p /opt/app/datartr/spool/f
mkdir -p /opt/app/datartr/spool/n
mkdir -p /opt/app/datartr/spool/s
CLASSPATH=$ETC
for FILE in `find $LIB -name *.jar`; do
  CLASSPATH=$CLASSPATH:$FILE
done
java -classpath $CLASSPATH  org.onap.dmaap.datarouter.node.NodeRunner

runner_file="$LIB/datarouter-node-jar-with-dependencies.jar"
echo "Starting using" $runner_file
java -Dcom.att.eelf.logging.file=/opt/app/datartr/etc/logback.xml -Dcom.att.eelf.logging.path=/root \
-Dorg.onap.dmaap.datarouter.node.properties=/opt/app/datartr/etc/node.properties -jar $runner_file