#!/bin/sh

mvn install:install-file -Dfile=master-1.pom -DpomFile=master-1.pom
mvn install:install-file -Dfile=master-x.pom -DpomFile=master-x.pom
mvn install:install-file -Dfile=ejb-1.jar -DpomFile=ejb-1.pom
mvn install:install-file -Dfile=ejb-1-client.jar -Dclassifier=client -DpomFile=ejb-1.pom
mvn install:install-file -Dfile=delegate-1.jar -DpomFile=delegate-1.pom
