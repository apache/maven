#!/bin/sh

#
# WORKAROUND FOR http://jira.codehaus.org/browse/MNG-214
#

REPO=$HOME/repository-new

cp pom.xml pom.xml.backup
cat pom.xml.backup | sed 's#<packaging>maven-plugin</packaging>##' >pom.xml

#m2 clean:clean plugin:descriptor deploy

version=`cat $REPO/org/apache/maven/plugins/maven-assembly-plugin/1.0-SNAPSHOT/maven-assembly-plugin-1.0-SNAPSHOT.version.txt`
cat pom.xml.backup | sed "s#<version>1.0-SNAPSHOT</version>#<version>$version</version>#" >pom.xml
scp pom.xml maven@beaver.codehaus.org:/home/projects/maven/repository-staging/to-ibiblio/maven2/org/apache/maven/plugins/maven-assembly-plugin/1.0-SNAPSHOT/maven-assembly-plugin-$version.pom

mv pom.xml.backup pom.xml
