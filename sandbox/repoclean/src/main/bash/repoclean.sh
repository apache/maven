#!/bin/bash

CP=./lib/repoclean-1.0-SNAPSHOT.jar

for lib in `ls -1 ./lib | grep -v 'repoclean'`
do

  CP=$CP:./lib/$lib

done

cygwin=false
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

if [ $cygwin == true ]; then
  CP=`cygpath -pw $CP`
fi

JAVA_OPTS=""

if [ "$1" == "profile" ]; then
  JAVA_OPTS="-agentlib:yjpagent=onexit=memory"
  # You need to customise this path for your environment
  export PATH=$PATH:/usr/local/yourkit/bin/win32
  shift
fi

if [ -z "$JAVA_HOME" ]; then
  JAVA_HOME=/usr/local/java

nice -n 19 $JAVA_HOME/bin/java -Xmx128M -Xms64M -Xincgc $JAVA_OPTS -classpath ${CP} org.apache.maven.tools.repoclean.Main $* | tee repoclean-log.txt
