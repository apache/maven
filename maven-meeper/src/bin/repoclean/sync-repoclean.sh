#!/bin/bash

dest=/home/maven/repository-staging/to-ibiblio
repocleanhome=$HOME/repository-tools/repoclean

$repocleanhome/repoclean.sh ~/components/src/bin/repoclean/synchronize.properties

rsync --ignore-existing -ripl $dest/maven2-repoclean/ $dest/maven2/
