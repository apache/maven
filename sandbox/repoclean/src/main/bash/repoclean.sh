#!/bin/bash

mypath=`which $0`

echo "mypath: $mypath"

mydir=`pwd`

if [ "" != "$mypath" ]; then

  echo "Setting mydir based on mypath..."
  mydir=`dirname $mypath`

fi

echo "mydir: $mydir"

CP=$mydir/lib/repoclean-1.0-SNAPSHOT.jar

for lib in `ls -1 $mydir/lib | grep -v 'repoclean'`
do

  CP=$CP:$mydir/lib/$lib

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
fi

nice -n 19 $JAVA_HOME/bin/java -Xmx128M -Xms64M -Xincgc $JAVA_OPTS -classpath ${CP} org.apache.maven.tools.repoclean.Main $* | tee repoclean-log.txt
