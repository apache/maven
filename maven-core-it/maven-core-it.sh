#!/bin/sh

# This process assumes that maven-core-it-verifier has been built.

cp=../maven-core-it-verifier/target/maven-core-it-verifier-1.0.jar

verifier=org.apache.maven.it.Verifier

# TODO: need a consistent way to discover M2_HOME across this, bootstrap and m2 itself, as well as have a sensible
# default, and a way to override. There must be only one way.
# I like the idea of using the one in the path, or using -Dmaven.home to override
# The m2 shell script should not care what installation it is in - it should use the installation defined on the
# command line

jvm_args="$@"

if [ ! -z "$M2_HOME" ]; then
  jvm_args="$jvm_args -Dmaven.home=$M2_HOME"
fi

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
fi

java $jvm_args -cp "$cp" $verifier

