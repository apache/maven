#!/bin/sh

echo This script is very temporary. Please validate all input files in the source repository before blindly copying them in.
echo Ensure all artifacts have a valid POM.
echo This will be removed when the repository manager is in place.

echo

echo
echo For a better explanation of the output flags please check --itemize-changes at rsync man page
echo

if [ "$1" == "go" ]; then
  echo Doing sync for real
else
  echo Not syncing
  RSYNC_OPTS="$RSYNC_OPTS -n"
fi

BASEDIR=$HOME/repository-staging/to-ibiblio/maven2
CHANGED_LOG=/tmp/sync-changed.log
rm $CHANGED_LOG

for f in `find conf -iname "*.sh"`
  do

  FROM=
  TO=
  NO_SSH=
  SSH_OPTS=
  RSYNC_SSH=

  source $f

  if [ -z $NO_SSH ]
  then
    if [ -z $SSH_OPTS ]
    then
      RSYNC_SSH="-e ssh"
    else
      RSYNC_SSH="-e \"ssh $SSH_OPTS\""
    fi
  fi

  echo "Syncing $FROM -> $TO"
  rsync --include=*/ --include=**/maven-metadata.xml* --exclude=* --exclude-from=$HOME/components/maven-meeper/src/bin/syncopate/exclusions.txt $RSYNC_OPTS -Lrtivz $RSYNC_SSH $FROM $BASEDIR/$TO
  rsync --exclude-from=$HOME/components/maven-meeper/src/bin/syncopate/exclusions.txt --ignore-existing $RSYNC_OPTS -Lrtivz $RSYNC_SSH $FROM $BASEDIR/$TO

  # check for changed files
  rsync -n --exclude=**/maven-metadata.xml* --exclude-from=$HOME/components/maven-meeper/src/bin/syncopate/exclusions.txt --existing $RSYNC_OPTS -Lrtivzc $RSYNC_SSH $FROM $BASEDIR/$TO >> $CHANGED_LOG

done

echo "*******************************************************************************"
echo "*******************************  CHANGED FILES  *******************************"
echo "*******************************************************************************"
cat $CHANGED_LOG
echo "*******************************************************************************"
echo "*******************************************************************************"
echo "*******************************************************************************"

