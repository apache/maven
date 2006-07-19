#!/bin/bash

dir=/home/maven/repository-staging/to-ibiblio
repocleanhome=$HOME/repository-tools/repoclean
log=$repocleanhome/last-changes-java.net.log

src=maven2-repoclean-java.net/com/sun
dst=maven2/com/sun

cd $dir/maven-java.net

cvs update -P

$repocleanhome/repoclean.sh ~/components/maven-meeper/src/bin/repoclean/java.net/synchronize.properties

rsync --ignore-existing -rvpl $dir/$src/ $dir/$dst/ > $log

for f in `cat $log | grep maven-metadata.xml$` ; do
  md5sum $dir/$dst/$f > $dir/$dst/$f.md5;
  sha1sum $dir/$dst/$f > $dir/$dst/$f.sha1;
  md5sum $dir/$src/$f > $dir/$src/$f.md5;
  sha1sum $dir/$src/$f > $dir/$src/$f.sha1;
done
