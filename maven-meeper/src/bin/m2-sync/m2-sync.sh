#!/bin/sh

echo This script is very temporary. Please validate all input files in the source repository before blindly copying them in.
echo Ensure all artifacts have a valid POM, and are signed used PGP.
echo This will be removed when the repository manager is in place.

echo

echo
echo If you see a "c" flag in the output for something different than maven-metadata files
echo then we have a problem, because it means the checksum of the file changed
echo
echo For a better explanation of the output flags please check --itemize-changes at rsync man page
echo

OPTS=-n
if [ "$1" == "go" ]; then
  echo Doing sync for real
  OPTS=
else
  echo Not syncing
fi

cd $HOME/repository-staging/to-ibiblio/maven2

# ideally we would use --ignore-existing but we need to copy the metadata files

CMD="rsync --exclude-from=$HOME/repository-tools/syncopate/exclusions.txt -e ssh $OPTS -acivz $FROM $TO"
echo "Syncing $FROM -> $TO"
$CMD
