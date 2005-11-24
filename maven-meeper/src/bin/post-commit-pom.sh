#!/bin/sh

set -x

svnlook=/usr/local/subversion/bin/svnlook 
repos=/home/projects/maven/repository-staging/to-ibiblio/maven2

$svnlook changed -r $2 $1 | egrep '\.pom$' | while read t1 t2
do
  mkdir -p `dirname $repos/$t2`
  $svnlook cat -r $2 $1 $t2 >$repos/$t2
  sha1sum $repos/$t2 >$repos/$t2.sha1
  md5sum $repos/$t2 >$repos/$t2.md5
done

