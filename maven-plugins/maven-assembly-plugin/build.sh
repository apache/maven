#!/bin/sh

#
# WORKAROUND FOR http://jira.codehaus.org/browse/MNG-214
#


REPO=$HOME/repository-new

cp pom.xml pom.xml.backup
cat pom.xml.backup | sed 's#<packaging>maven-plugin</packaging>##' >pom.xml

m2 clean:clean plugin:descriptor install

mv pom.xml.backup pom.xml
cp pom.xml $REPO/org/apache/maven/plugins/maven-assembly-plugin/1.0-SNAPSHOT/maven-assembly-plugin-1.0-SNAPSHOT.pom

