#!/bin/sh

# This process assumes that maven-core-it-verifier has been built.

home=`pwd`

cp=`pwd`/../maven-core-it-verifier/target/maven-core-it-verifier-1.0.jar

#if $cygwin; then
#  cp=`cygpath -pw "$cp"`
#fi

if [ ! -f $cp ]
then
  echo 
  echo "The verifier needs to be built ... "
  echo
  ( cd ../maven-core-it-verifier; mboot --install )  
fi

verifier=org.apache.maven.it.Verifier

integration_tests=`cat integration-tests.txt`

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
      ./prebuild.hook
      echo
    fi
    
    m2 clean:clean `cat goals.txt`
    
    if [ -f postbuild.hook ]
    then    
      echo
      ./postbuild.hook
      echo
    fi
    
    basedir=`pwd`
    
    #if $cygwin; then
    #  basedir=`cygpath -pw "$basedir"`
    #fi
    
    java -cp "$cp" $verifier "$basedir"
  )
done
