#!/bin/sh

# This process assumes that maven-core-it-verifier has been built.

cp=../maven-core-it-verifier/target/maven-core-it-verifier-1.0.jar

verifier=org.apache.maven.it.Verifier

jvm_args="$@"

if [ ! -z "$MAVEN_OPTS" ]; then
  jvm_args="$jvm_args $MAVEN_OPTS"
fi

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
  jvm_args="$jvm_args -Dmaven.home=$M2_HOME"
fi

java $jvm_args -cp "$cp" $verifier

