#!/bin/sh

# ----------------------------------------------------------------------------------

PID=$$
RUNNING=`ps -ef | grep checkrepo.sh | grep -v 'sh -c' | grep -v grep | grep -v $PID`
if [ ! -z "$RUNNING" ]; then
  echo "checkrepo.sh already running... exiting"
  exit 1
fi

REPO=$HOME/repository-staging/to-ibiblio/maven2

PRG="$0"
PRGDIR=`dirname "$PRG"`

(
cd $PRGDIR/..
PRGDIR=`pwd`
CLASSPATH=$PRGDIR/lib/maven-repository-checker-1.0-SNAPSHOT.jar:$PRGDIR/lib/maven-model-2.0-beta-1-SNAPSHOT.jar:$PRGDIR/lib/plexus-utils-1.0.1.jar
$JAVA_HOME/bin/java -classpath $CLASSPATH org.apache.maven.repository.checker.CheckRepo $REPO
)
