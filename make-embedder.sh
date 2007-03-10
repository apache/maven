#!/bin/sh

ds=`date "+2.1.0.v%Y%m%d-%H%M"`
dir=`pwd`

mvn source:aggregate
mv target/maven-2.1-SNAPSHOT-sources.jar $dir/maven-embedder-$ds-depsrc.zip

(

  cd maven-embedder
  sed "s@<bundleVersion>.*</bundleVersion>@<bundleVersion>$ds</bundleVersion>@" pom.xml > tmp; mv tmp pom.xml
  mvn clean install
  mv target/maven-embedder-*-ueber.jar $dir/maven-embedder-$ds-dep.jar
)
