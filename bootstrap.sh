#!/bin/sh

# TODO: error checking

(
  cd bootstrap/bootstrap-mini
  ./build
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
  java -jar target/bootstrap-mini.jar install
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

BOOTSTRAP_JAR=../bootstrap-mini/target/bootstrap-mini-2.0.1-SNAPSHOT.jar

(
  cd bootstrap/bootstrap-installer
  java -jar $BOOTSTRAP_JAR package
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

java -jar bootstrap/bootstrap-installer/target/bootstrap-installer-2.0.1-SNAPSHOT.jar
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

