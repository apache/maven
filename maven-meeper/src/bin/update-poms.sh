#!/bin/sh

src=$HOME/maven/repository-staging/pom-svn-repository
dest=$HOME/maven/repository-staging/to-ibiblio/maven2

/usr/local/subversion/bin/svn update $src

./copy-updated-poms.sh
