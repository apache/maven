#!/bin/sh

#
# WORKAROUND FOR http://jira.codehaus.org/browse/MNG-214
#

m2 plugin:descriptor
m2 modello:xpp3-reader modello:xpp3-writer modello:java resources:resources compiler:compile resources:testResources compiler:testCompile surefire:test jar:jar
cp target/maven-assemble-plugin-1.0-SNAPSHOT.jar $HOME/repository-m2/org.apache.maven.plugins/maven-plugins
