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

if [ "$1" == "go" ]; then
  echo Doing sync for real
else
  echo Not syncing
  RSYNC_OPTS="$RSYNC_OPTS -n"
fi

cd $HOME/repository-staging/to-ibiblio/maven2

# ideally we would use --ignore-existing but we need to copy the metadata files

echo "Syncing $FROM -> $TO"
rsync --exclude-from=$HOME/components/maven-meeper/src/bin/syncopate/exclusions.txt $RSYNC_OPTS -Lrtivz -e "ssh $SSH_OPTS" $FROM $TO
