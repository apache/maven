#!/bin/sh

# Check to make sure  JAVA_HOME is set
[ -z $JAVA_HOME ] && echo && echo 'You must set $JAVA_HOME to use mboot!' && echo && exit 1

ARGS="$@"

if [ ! -z "$M2_HOME" ]; then
  ARGS="$ARGS -Dmaven.home=$M2_HOME"
fi

# Build and install mboot
(
  echo "-----------------------------------------------------------------------"
  echo " Building mboot ... "
  echo "-----------------------------------------------------------------------"  

  cd ./maven-mboot2
  ./build $ARGS
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

(
  echo "-----------------------------------------------------------------------"
  echo " Building maven2 components ... "
  echo "-----------------------------------------------------------------------"  

  $JAVA_HOME/bin/java $ARGS -jar mboot.jar
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

(
  cd ./maven-core-it
  echo
  echo "Running maven-core integration tests ..."
  echo 
  ./maven-core-it.sh $ARGS
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi
