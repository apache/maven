#!/bin/sh

mydir=`dirname $0`

CP=`ls -1 $mydir/lib/*.jar | tr '\n' ':'`

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

$JAVA_HOME/bin/java -Xmx128M -Xms64M -Xincgc $JAVA_OPTS -classpath ${CP} org.apache.maven.tools.repoclean.Main $*
