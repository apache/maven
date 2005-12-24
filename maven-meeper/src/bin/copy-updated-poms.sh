#!/bin/sh

GROUPID=$1

src=/home/projects/maven/repository-staging/pom-svn-repository
dest=/home/projects/maven/repository-staging/to-ibiblio/maven2


if [ ! -z $GROUPID ]
then
  src=$src/$GROUPID
  dest=$src/$GROUPID
fi


rsync -e ssh -v -rpt --exclude=.svn --exclude=updated-poms.log $src/ $dest/ > $src/updated-poms.log

for f in `grep .pom $src/updated-poms.log` ; do 
  md5sum $dest/$f > $dest/$f.md5 ; 
  sha1sum $dest/$f > $dest/$f.sha1;
done
