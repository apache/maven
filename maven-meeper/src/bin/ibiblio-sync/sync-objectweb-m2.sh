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
CMD="rsync --exclude="README.txt" -e ssh $OPTS -avz maven@forge.objectweb.org:../../groups/maven/htdocs/maven2/org/objectweb/* org/objectweb/"
echo $CMD
$CMD
