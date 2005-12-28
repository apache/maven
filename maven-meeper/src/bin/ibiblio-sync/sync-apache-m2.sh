#!/bin/sh

echo This script is very temporary. Please validate all input files in the apache repository before blindly copying them in.
echo Ensure all artifacts have a valid POM, and are signed used PGP.
echo This will be removed when the repository manager is in place.

OPTS=-n
if [ "$1" == "go" ]; then
  echo Doing sync for real
  OPTS=
fi

cd $HOME/repository-staging/to-ibiblio/maven2
echo rsync --exclude="README.txt" -e ssh $OPTS -avz jvanzyl@cvs.apache.org:/www/www.apache.org/dist/maven-repository/* .
rsync --exclude="README.txt" -e ssh $OPTS -avz jvanzyl@cvs.apache.org:/www/www.apache.org/dist/maven-repository/* .
