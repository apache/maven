#!/bin/sh

src=/home/projects/maven/repository-staging/pom-svn-repository
dest=/home/projects/maven/repository-staging/to-ibiblio/maven2

rsync -e ssh -v -rpt --exclude=.svn --exclude=updated-poms.log $src/ $dest/ > updated-poms.log

for f in `grep .pom updated-poms.log` ; do 
  md5sum $dest/$f > $dest/$f.md5 ; 
  sha1sum $dest/$f > $dest/$f.sha1;
done

rm updated-poms.log
