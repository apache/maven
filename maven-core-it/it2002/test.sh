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

m2 -e release:prepare -Denv=test

m2 -e release:perform -Denv=test

