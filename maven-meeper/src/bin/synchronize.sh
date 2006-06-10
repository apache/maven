#!/bin/sh

PID=$$
RUNNING=`ps -ef | grep synchronize.sh | grep -v 'sh -c' | grep -v grep | grep -v $PID`
if [ ! -z "$RUNNING" ]; then
  echo Sync already running... exiting
  echo $RUNNING
  exit 1
fi


TOOLS_BASE=$HOME/repository-tools
(
  cd $TOOLS_BASE/syncopate
  ./sync
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi

(
  cd $TOOLS_BASE/repoclean
  ./repoclean.sh synchronize.properties
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi


# hack prevent commons-logging-1.1-dev
CL=$HOME/repository-staging/to-ibiblio/maven2/commons-logging/commons-logging
rm -rf $CL/1.1-dev
grep -v 1.1-dev $CL/maven-metadata.xml > $CL/maven-metadata.xml.tmp
mv $CL/maven-metadata.xml.tmp $CL/maven-metadata.xml
md5sum $CL/maven-metadata.xml > $CL/maven-metadata.xml.md5
sha1sum $CL/maven-metadata.xml > $CL/maven-metadata.xml.sha1


(
  cd $TOOLS_BASE/ibiblio-sync
  ./synchronize-codehaus-to-ibiblio.sh
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi

