#!/bin/bash

dir=/home/maven/repository-staging/to-ibiblio
repocleanhome=$HOME/repository-tools/repoclean
log=$repocleanhome/last-changes.log

src=maven2-repoclean
dst=maven2

$repocleanhome/repoclean.sh ~/components/maven-meeper/src/bin/repoclean/synchronize.properties

rsync --ignore-existing -rvpl $dir/$src/ $dir/$dst/ > $log

for f in `cat $log | grep maven-metadata.xml$` ; do
  md5sum $dir/$dst/$f > $dir/$dst/$f.md5;
  sha1sum $dir/$dst/$f > $dir/$dst/$f.sha1;
  md5sum $dir/$src/$f > $dir/$src/$f.md5;
  sha1sum $dir/$src/$f > $dir/$src/$f.sha1;
done
