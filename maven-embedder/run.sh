#!/bin/sh

m2 clean:clean assembly:assembly

CLASSPATH=target/maven-embedder-2.0-beta-2-SNAPSHOT-dep.jar

javac -classpath $CLASSPATH Plugin.java

java -classpath $CLASSPATH:. Plugin
