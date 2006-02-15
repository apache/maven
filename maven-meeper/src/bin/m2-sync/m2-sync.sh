#!/bin/sh

echo This script is very temporary. Please validate all input files in the source repository before blindly copying them in.
echo Ensure all artifacts have a valid POM, and are signed used PGP.
echo This will be removed when the repository manager is in place.

OPTS=-n
if [ "$1" == "go" ]; then
  echo Doing sync for real
  OPTS=
else
  echo Not syncing
fi

cd $HOME/repository-staging/to-ibiblio/maven2
CMD="rsync --ignore-existing --exclude-from=/home/projects/maven/repository-tools/syncopate/exclusions.txt -e ssh $OPTS -avz $FROM $TO"
echo "Syncing $FROM -> $TO"
$CMD
