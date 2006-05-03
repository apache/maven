#!/bin/bash

rm -Rf target

mkdir target

dir=`readlink -f ${PWD}`

if [ "$1" == "windows" ]; then
  dir=`cygpath -m $dir`
  echo setting dir to $dir
fi

export CVSROOT=:ext:localhost:$dir/target/cvsroot

cvs init
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

cd cvs-project
cvs import -I '.svn' -m "import." project ASF INIT
ret=$?; if [ $ret != 0 ]; then exit $ret; fi
cd ..

cvs co -d target/project.checkout project
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

(
cd target/project.checkout

cat pom.xml | sed "s#\${project.file.parentFile.parentFile}#$dir#g" >tmp
mv tmp pom.xml

cvs ci -m 'update scm' pom.xml
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

rm -Rf target

mvn clean install
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

mvn -DgenerateReleasePoms=true -e release:prepare -Denv=test -B -Dtag=test-tag
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

mvn -DreleasePom=release-pom.xml -e release:perform -Denv=test
ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
