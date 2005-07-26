#!/bin/sh

# Check to make sure  JAVA_HOME is set
[ -z "$JAVA_HOME" ] && echo && echo 'You must set $JAVA_HOME to use mboot!' && echo && exit 1

JAVACMD=$JAVA_HOME/bin/java

ARGS="$@"

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$M2_HOME" ] && M2_HOME=`cygpath -w "$M2_HOME"`
fi

if [ -z "$M2_HOME" ]; then
  echo "M2_HOME must be set."
  exit 1
fi

HOME_ARGS="-Dmaven.home=$M2_HOME"

# Build and install mboot
(
  echo "-----------------------------------------------------------------------"
  echo " Building mboot ... "
  echo "-----------------------------------------------------------------------"  

  cd ./maven-mboot2
  ./build
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

(
  echo "-----------------------------------------------------------------------"
  echo " Building maven2 components ... "
  echo "-----------------------------------------------------------------------"  

  "$JAVACMD" "$HOME_ARGS" $MAVEN_OPTS -jar mboot.jar $ARGS
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

# I Really Don't want to be rebuilding these (Especially the reports) every time, but
# until we regularly push them to the repository and the integration tests rely on
# some of these plugins, there is no choice
(
  echo "-----------------------------------------------------------------------"
  echo " Rebuilding maven2 plugins ... "
  echo "-----------------------------------------------------------------------"  

  cd maven-plugins
  # update the release info to ensure these versions get used in the integration tests
  m2 --no-plugin-registry --batch-mode -DupdateReleaseInfo=true -e $ARGS clean:clean install
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

(
  cd ./maven-core-it
  echo
  echo "Running maven-core integration tests ..."
  echo 
  ./maven-core-it.sh
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi
