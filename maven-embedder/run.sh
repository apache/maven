#!/bin/sh

mvn clean:clean assembly:assembly

CLASSPATH=target/maven-embedder-2.0-dep.jar

javac -classpath $CLASSPATH Plugin.java

java -classpath $CLASSPATH:. Plugin
