#!/bin/bash

CP=./lib/repoclean-1.0-SNAPSHOT.jar
CP=$CP:./lib/plexus-container-default-1.0-alpha-2-SNAPSHOT.jar
CP=$CP:./lib/plexus-utils-1.0-alpha-2-SNAPSHOT.jar
CP=$CP:./lib/classworlds-1.1-alpha-1.jar
CP=$CP:./lib/maven-artifact-2.0-SNAPSHOT.jar
CP=$CP:./lib/maven-model-2.0-SNAPSHOT.jar
CP=$CP:./lib/wagon-provider-api-1.0-alpha-2-SNAPSHOT.jar
CP=$CP:./lib/wagon-file-1.0-alpha-2-SNAPSHOT.jar
CP=$CP:./lib/wagon-http-lightweight-1.0-alpha-2-SNAPSHOT.jar

java -classpath ${CP} org.apache.maven.tools.repoclean.Main $* | tee repoclean-log.txt
