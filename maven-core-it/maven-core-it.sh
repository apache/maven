#!/bin/sh

# This process assumes that maven-core-it-verifier has been built.

home=`pwd`

cp=../../maven-core-it-verifier/target/maven-core-it-verifier-1.0.jar

verifier=org.apache.maven.it.Verifier

integration_tests=`cat integration-tests.txt | egrep -v '^#'`

for integration_test in $integration_tests
do
  (
    cd $integration_test
    
    if [ -f prebuild.hook ]
    then
      echo      
       sh prebuild.hook
      echo
    fi
    
    m2 clean:clean `cat goals.txt`
    
    if [ -f postbuild.hook ]
    then    
      echo
      sh postbuild.hook
      echo
    fi
    
    basedir=.
    
    java -cp "$cp" $verifier "$basedir"        
  ) > ${integration_test}-log.txt

  if [ "$?" = "0" ]
  then
    echo "Integration test $integration_test OK"
  else
    echo "Integration test $integration_test FAILED!"
    echo "Details:"
    cat ${integration_test}-log.txt
    echo
  fi

done
