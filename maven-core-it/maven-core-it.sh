#!/bin/sh

home=`pwd`

cp=`pwd`/../maven-core-it-verifier/target/maven-core-it-verifier-1.0.jar
verifier=org.apache.maven.it.Verifier

integration_tests=`cat integration-tests.txt`

for integration_test in $integration_tests
do
  echo "Running integration test $integration_test ..."
  
  (
    cd $integration_test
   
    m2 clean jar
    
    java -cp $cp $verifier `pwd`
  )
done
