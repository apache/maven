#!/bin/bash

dest=/home/maven/repository-staging/to-ibiblio
repocleanhome=$HOME/repository-tools/repoclean

cd $dest/maven-java.net

cvs update -P

$repocleanhome/repoclean.sh ~/components/maven-meeper/src/bin/repoclean/java.net/synchronize.properties

rsync --ignore-existing -ripl $dest/maven2-repoclean-java.net/com/sun/ $dest/maven2/com/sun/
