#!/bin/bash
LIB=/opt/app/subscriber/lib
ETC=/opt/app/subscriber/etc
CLASSPATH=$ETC
for FILE in `find $LIB -name *.jar`; do
  CLASSPATH=$CLASSPATH:$FILE
done
java -classpath $CLASSPATH  org.onap.dmaap.datarouter.subscriber.SubscriberMain

runner_file="$LIB/subscriber-jar-with-dependencies.jar"
echo "Starting using" $runner_file
java -Dorg.onap.dmaap.datarouter.subscriber.properties=/opt/app/subscriber/etc/subscriber.properties -jar $runner_file