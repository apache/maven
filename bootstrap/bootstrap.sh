#!/bin/sh

# TODO: error checking

(
  cd bootstrap-mini
  ./build
  java -jar target/bootstrap-mini.jar install
)

BOOTSTRAP_JAR=../bootstrap-mini/target/bootstrap-mini-2.0.1-SNAPSHOT.jar

(
  cd bootstrap-installer
  java -jar $BOOTSTRAP_JAR package
)

(
  cd ..
  java -classpath bootstrap/bootstrap-installer/target/bootstrap-installer-2.0.1-SNAPSHOT.jar:bootstrap/bootstrap-mini/target/bootstrap-mini-2.0.1-SNAPSHOT.jar org.apache.maven.bootstrap.installer.BootstrapInstaller
)

