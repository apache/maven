#!/bin/sh

# This process assumes that maven-core-it-verifier has been built.

home=`pwd`

cp=../../maven-core-it-verifier/target/maven-core-it-verifier-1.0.jar

verifier=org.apache.maven.it.Verifier

integration_tests=`cat integration-tests.txt | egrep -v '^#'`

for integration_test in $integration_tests
do

  echo "----------------------------------------------------------"
  echo "Running integration test $integration_test ..."
  echo "----------------------------------------------------------"  
  (
    cd $integration_test
    
    if [ -f prebuild.hook ]
    then
      echo      
       sh prebuild.hook
      echo
    fi
    
    m2 clean:clean `cat goals.txt`
    ret=$?; if [ $ret != 0 ]; then exit $ret; fi
    
    if [ -f postbuild.hook ]
    then    
      echo
      sh postbuild.hook
      echo
    fi
    
    basedir=.
    
    java -cp "$cp" $verifier "$basedir"
    ret=$?; if [ $ret != 0 ]; then exit $ret; fi
  )
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
done
