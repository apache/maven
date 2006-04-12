#!/bin/sh

m2Versions="2.0.1 2.0.2 2.0.3 2.0.4"

for i in $m2Versions
do
  echo "Testing against version $i"
  M2_HOME=$HOME/maven-${i}
  mvn clean test
done
