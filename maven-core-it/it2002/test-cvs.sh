#!/bin/bash

rm -Rf target

mkdir target

cvs -d target/cvsroot init

dir=`readlink -f ${PWD}`

if [ "$1" == "windows" ]; then
  dir=`cygpath -m $dir`
  echo setting dir to $dir
fi

export CVSROOT=$PWD/target/cvsroot

cvs import cvs-project -m "import." project ASF INIT

cvs co -d target/project.checkout project

cd target/project.checkout

cat pom.xml | sed "s#\${project.file.parentFile.parentFile}#$dir#g" >tmp
mv tmp pom.xml

cvs ci -m 'update scm' pom.xml

rm -Rf target

mvn clean install
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

mvn -DgenerateReleasePoms=true -e release:prepare -Denv=test -B -Dtag=test-tag
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

mvn -DreleasePom=release-pom.xml -e release:perform -Denv=test
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

