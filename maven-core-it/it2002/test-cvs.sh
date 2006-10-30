#!/bin/bash


echo
echo "*********************************************************"
echo if you are running in windows, run $0 windows
echo "*********************************************************"
echo


rm -Rf target

mkdir target

cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true ;;
esac

if $darwin; then
  dir=$PWD
else
  dir=`readlink -f ${PWD}`
fi

if $cygwin; then
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

cd target
cvs co -d project.checkout project
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

(
cd project.checkout

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
