#!/bin/sh
java -cp `cygpath -pw $HOME/repository/maven/jars/pom-pre-alpha-converter-1.0.jar:/home/Brett/repository/plexus/jars/plexus-utils-1.0-alpha-1.jar:$HOME/repository/maven/jars/maven-model-4.0.0-pre-alpha1.jar:$HOME/repository/maven/jars/maven-model-2.0-SNAPSHOT.jar` org.apache.maven.tools.converter.Main "$@"
