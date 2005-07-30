#!/bin/sh

# This process assumes that maven-core-it-verifier has been built.

cp=../maven-core-it-verifier/target/maven-core-it-verifier-1.0.jar

verifier=org.apache.maven.it.Verifier

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath -w "$JAVA_HOME"`
  export JAVA_HOME
  [ -n "$M2_HOME" ] && M2_HOME=`cygpath -w "$M2_HOME"`
fi

if [ ! -z "$M2_HOME" ]; then
  jvm_m2_home="-Dmaven.home=$M2_HOME"
fi

# Don't debug the verifier, debug m2
opts=`echo $MAVEN_OPTS | sed 's/-Xdebug//' | sed 's/-Djava.compiler=NONE//' | sed 's/-Xnoagent//' | sed 's/-Xrunjdwp[^ ]*//'`

# If you want to debug the verifier, make --debug the first argument
if [ "$1" = "--debug" ]; then
  shift
  opts="$opts -Xdebug -Djava.compiler=NONE -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
  echo Debugging verifier on port 5005
fi

java "$jvm_m2_home" $opts -cp "$cp" $verifier $@

