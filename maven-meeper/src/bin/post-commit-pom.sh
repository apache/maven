#!/bin/sh

set -x

svnlook=/usr/local/subversion/bin/svnlook 
repos=$HOME/repository-staging/to-ibiblio/maven2

# TODO: handle deletions
$svnlook changed -r $2 $1 | egrep '\.pom$' | while read t1 t2
do
  file=$repos/`echo $t2 | sed 's/^repository\///'`
  mkdir -p `dirname $file`
  $svnlook cat -r $2 $1 $t2 >$file
  sha1sum $file >$file.sha1
  md5sum $file >$file.md5
  chgrp maven $file $file.md5 $file.sha1
done

