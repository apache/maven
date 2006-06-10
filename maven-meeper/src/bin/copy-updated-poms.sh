#!/bin/sh

GROUPID=$1

basesrc=$HOME/repository-staging/pom-svn-repository
basedest=$HOME/repository-staging/to-ibiblio/maven2


if [ ! -z $GROUPID ]
then
  src=$basesrc/$GROUPID
  dest=$basedest/$GROUPID
else
  src=$basesrc
  dest=$basedest
fi


rsync -e ssh -v -rpt --exclude=.svn --exclude=updated-poms.log $src/ $dest/ > $basesrc/updated-poms.log

for f in `grep .pom $basesrc/updated-poms.log` ; do 
  md5sum $dest/$f > $dest/$f.md5 ; 
  sha1sum $dest/$f > $dest/$f.sha1;
done
