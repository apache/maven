#!/bin/bash

rm -Rf target
rm -Rf project.checkout

mkdir target

svnadmin create --fs-type fsfs target/svnroot

dir=`readlink -f ${PWD}`

svn import project file://${dir}/target/svnroot/project/trunk -m "import."
svn mkdir file://${dir}/target/svnroot/project/tags -m "Creating tags dir."

svn co file://${dir}/target/svnroot/project/trunk project.checkout

cd project.checkout

rm -Rf target

#. ~/shell-switches/m2-debug-on
#echo "Enabling debugging options. Please attach the debugger."

export MAVEN_OPTS=
m2 -e release:prepare

m2 -e release:perform

