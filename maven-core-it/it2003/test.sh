#!/bin/bash

RESULT=`mvn -X clean | grep 'org.apache.maven.plugins:maven-clean-plugin:maven-plugin:2.0'`

if [ ${RESULT} == "" ]; then
  echo "Incorrect version of maven-clean-plugin detected. Test failed."
  exit 1;
else
  echo "Found correct version of maven-clean-plugin (2.0) in line:"
  echo ""
  echo $RESULT
  echo ""
fi

