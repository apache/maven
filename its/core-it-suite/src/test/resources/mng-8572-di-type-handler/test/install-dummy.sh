#!/bin/sh
# Install the dummy artifact with our custom type
mvn install:install-file \
  -Dfile=src/main/java/org/apache/maven/its/mng8572/test/DummyClass.java \
  -DpomFile=dummy-artifact-pom.xml \
  -Dpackaging=custom-type \
  -DcreateChecksum=true
