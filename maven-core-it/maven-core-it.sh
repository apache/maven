#!/bin/sh

# This process assumes that maven-core-it-verifier has been built.

home=`pwd`

cp=../../maven-core-it-verifier/target/maven-core-it-verifier-1.0.jar

verifier=org.apache.maven.it.Verifier

integration_tests=`cat integration-tests.txt | egrep -v '^#'`

# TODO: this is rubbish. Let's rewrite this in java
local_repo=`cat $HOME/.m2/pom.xml | tr '\n' ' ' | sed 's/^.*<local> *<repository>//' | sed 's#</repository> *</local>.*$##'`

for i in "$@"
do
 j=`echo $i | sed 's/^-Dmaven.repo.local=//'`
 if [ "$i" != "$j" ]; then
  local_repo=$j
 fi
done

for integration_test in $integration_tests
do
  (
    cd $integration_test
    
    if [ -f prebuild.hook ]
    then
      echo      
       sh prebuild.hook "$local_repo"
      echo
    fi
    
    m2 -Dmaven.repo.local="$local_repo" clean:clean `cat goals.txt`
    
    if [ -f postbuild.hook ]
    then    
      echo
      sh postbuild.hook
      echo
    fi
    
    basedir=.
    
    java -cp "$cp" $verifier "$basedir" "$local_repo"
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

