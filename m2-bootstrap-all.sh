#!/bin/sh

# Check to make sure  JAVA_HOME is set
[ -z $JAVA_HOME ] && echo && echo 'You must set $JAVA_HOME to use mboot!' && echo && exit 1

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

  $JAVA_HOME/bin/java -jar mboot.jar
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
