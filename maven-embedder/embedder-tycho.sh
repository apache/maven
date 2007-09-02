#!/bin/sh

ds=`date "+2.1.0.v%Y%m%d-%H%M"`
dir=`pwd`

mvn clean

(
  cd ..
  mvn source:aggregate
  mkdir -p $dir/target
  mv target/maven-2.1-SNAPSHOT-sources.jar $dir/target/maven-embedder-tycho-$ds-depsrc.zip
)

sed "s@<bundleVersion>.*</bundleVersion>@<bundleVersion>$ds</bundleVersion>@" pom.xml > tmp; mv tmp pom.xml
mvn -Dmaven.test.skip=true -Ptycho package
