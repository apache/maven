#!/bin/bash

CP=./lib/repoclean-1.0-SNAPSHOT.jar
CP=$CP:./lib/plexus-container-default-1.0-alpha-2-SNAPSHOT.jar
CP=$CP:./lib/plexus-utils-1.0-alpha-2-SNAPSHOT.jar
CP=$CP:./lib/classworlds-1.1-alpha-1.jar
CP=$CP:./lib/maven-artifact-2.0-SNAPSHOT.jar
CP=$CP:./lib/maven-model-2.0-SNAPSHOT.jar
CP=$CP:./lib/wagon-provider-api-1.0-alpha-2-SNAPSHOT.jar
CP=$CP:./lib/wagon-file-1.0-alpha-2-SNAPSHOT.jar
CP=$CP:./lib/wagon-http-lightweight-1.0-alpha-2-SNAPSHOT.jar

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

nice -n 19 java -Xmx128M -Xms64M -Xincgc $JAVA_OPTS -classpath ${CP} org.apache.maven.tools.repoclean.Main $* | tee repoclean-log.txt
