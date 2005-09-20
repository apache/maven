#!/bin/bash

rm -Rf target
rm -Rf project.checkout

mkdir target

svnadmin create --fs-type fsfs target/svnroot

dir=`readlink -f ${PWD}`

if [ "$1" == "windows" ]; then
  dir=`cygpath -m $dir`
  echo setting dir to $dir
fi

svn import project file://localhost/${dir}/target/svnroot/project/trunk -m "import."
svn mkdir file://localhost/${dir}/target/svnroot/project/tags -m "Creating tags dir."

svn co file://localhost/${dir}/target/svnroot/project/trunk project.checkout

cd project.checkout

cat pom.xml | sed "s#\${project.file.parentFile.parentFile}#$dir#g" >tmp
mv tmp pom.xml
svn ci -m 'update scm' pom.xml

rm -Rf target

m2 -e release:prepare -Denv=test -B -Dtag=test-tag

m2 -e release:perform -Denv=test

