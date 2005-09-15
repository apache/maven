#!/bin/bash

rm -Rf target
rm -Rf project.checkout

mkdir target

svnadmin create --fs-type fsfs target/svnroot

rm -Rf `find project -type d -name .svn`

dir=`readlink -f ${PWD}`

svn import project file://${dir}/target/svnroot/project/trunk -m "import."
svn mkdir file://${dir}/target/svnroot/project/tags -m "Creating tags dir."

svn co file://${dir}/target/svnroot/project/trunk project.checkout

cd project.checkout

rm -Rf target

#. ~/shell-switches/m2-debug-on
#echo "Enabling debugging options. Please attach the debugger."
m2 -e release:prepare


