#!/bin/sh

#
# WORKAROUND FOR http://jira.codehaus.org/browse/MNG-214
#


REPO=$HOME/repository-new

cp pom.xml pom.xml.backup
cat pom.xml.backup | sed 's#<packaging>maven-plugin</packaging>##' >pom.xml

m2 clean:clean plugin:descriptor package
cp target/maven-assemble-plugin-1.0-SNAPSHOT.jar $REPO/org/apache/maven/plugins/maven-assemble-plugin/1.0-SNAPSHOT

mv pom.xml.backup pom.xml
cp pom.xml $REPO/org/apache/maven/plugins/maven-assemble-plugin/1.0-SNAPSHOT/maven-assemble-plugin-1.0-SNAPSHOT.pom

