#!/bin/bash

dest=/home/maven/repository-staging/to-ibiblio
repocleanhome=$HOME/repository-tools/repoclean
log=$repocleanhome/last-changes-java.net.log

cd $dest/maven-java.net

cvs update -P

$repocleanhome/repoclean.sh ~/components/maven-meeper/src/bin/repoclean/java.net/synchronize.properties

rsync --ignore-existing -ripl $dest/maven2-repoclean-java.net/com/sun/ $dest/maven2/com/sun/ > $log

for f in `cat $log | grep maven-metadata.xml` ; do
  md5sum $dest/maven2/$f > $dest/maven2/$f.md5;
  sha1sum $dest/maven2/$f > $dest/maven2/$f.sha1;
  md5sum $dest/maven2-repoclean-java.net/$f > $dest/maven2-repoclean-java.net/$f.md5;
  sha1sum $dest/maven2-repoclean-java.net/$f > $dest/maven2-repoclean-java.net/$f.sha1;
done
