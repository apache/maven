#!/bin/sh

echo This script is very temporary. Please validate all input files in the repository before blindly copying them in.
echo Ensure all artifacts have a valid POM.
echo This will be removed when the repository manager is in place.

OPTS=-n
if [ "$1" == "go" ]; then
  echo Doing sync for real
  OPTS=
fi

cd $HOME/repository-staging/to-ibiblio/maven2
echo rsync --exclude="README.txt" -e ssh $OPTS -avz carlossg@shell.sourceforge.net:/home/groups/m/ma/maven-taglib/htdocs/m2repo/net/sourceforge/maven-taglib/* .
rsync --exclude="README.txt" -e ssh $OPTS -avz carlossg@shell.sourceforge.net:/home/groups/m/ma/maven-taglib/htdocs/m2repo/net/sourceforge/maven-taglib/* .
