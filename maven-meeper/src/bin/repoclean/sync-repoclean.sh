#!/bin/bash

export dest=/home/maven/repository-staging/to-ibiblio
export repocleanhome=$HOME/repository-tools/repoclean
log=$repocleanhome/last-changes.log

$repocleanhome/repoclean.sh ~/components/maven-meeper/src/bin/repoclean/synchronize.properties

rsync --ignore-existing -rvpl $dest/maven2-repoclean/ $dest/maven2/ > $log

for f in `cat $log | grep maven-metadata.xml` ; do
  md5sum $dest/maven2/$f > $dest/maven2/$f.md5;
  sha1sum $dest/maven2/$f > $dest/maven2/$f.sha1;
  md5sum $dest/maven2-repoclean/$f > $dest/maven2-repoclean/$f.md5;
  sha1sum $dest/maven2-repoclean/$f > $dest/maven2-repoclean/$f.sha1;
done
