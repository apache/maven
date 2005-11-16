#!/bin/sh

ARGS=$@
ORIG_ARGS=$ARGS

(
  cd bootstrap/bootstrap-mini
  ./build
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
  java -jar target/bootstrap-mini.jar install $ARGS
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

BOOTSTRAP_JAR=bootstrap-mini/target/bootstrap-mini.jar

(
  cd bootstrap/bootstrap-installer
  java -jar ../$BOOTSTRAP_JAR package $ARGS
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

#PLUGINS_DIR=../plugins
PLUGINS_DIR=maven-plugins
if [ -d $PLUGINS_DIR ]; then
  ARGS="$ARGS --build-plugins --plugins-directory=$PLUGINS_DIR"
fi

# TODO: get rid of M2_HOME once integration tests are in here
java -jar bootstrap/bootstrap-installer/target/bootstrap-installer.jar --prefix=`dirname $M2_HOME` $ARGS
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

ARGS=$ORIG_ARGS

(
  # TODO: should w ebe going back to the mini now that we have the real thing?
  cd maven-core-it-verifier
  java -jar ../bootstrap/$BOOTSTRAP_JAR package $ARGS
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

