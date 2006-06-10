#!/bin/sh

src=$HOME/repository-staging/pom-svn-repository
dest=$HOME/repository-staging/to-ibiblio/maven2

svn update $src

./copy-updated-poms.sh
