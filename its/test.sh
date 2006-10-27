#!/bin/sh

# Prototyping running the integration tests locally without a network connection given that you have
# all the dependencies required to run maven already downloaded. I've tried to make the integration
# testing completely self-contained. I'm using a shell script but this will eventually be 
# tied up with the invoker/embedder.

OPTS="--settings settings.xml -Pmode-local-offline"

# This will gather all the requirements for the integration tests. After downloading, build and
# installing all the modules you should have a local repository with everything you need so that
# you can work offline
#mvn --settings settings.xml install

# This should deploy all the integration testing artifacts into out filesystem-based, offline
# remote repository. This is just sitting in your filesystem.
rm -rf /tmp/mits/repository-remote
mvn $OPTS deploy

# Need to build the support artifacts and put them in the remote repo. I can't put these in a reactor
# because all the artifacts have the same id.
mvn $OPTS -f core-integration-testing-support/1.0/pom.xml deploy
mvn $OPTS -f core-integration-testing-support/1.1/pom.xml deploy
mvn $OPTS -f core-integration-testing-support/1.1-old-location/pom.xml deploy
mvn $OPTS -f core-integration-testing-support/1.2/pom.xml deploy
mvn $OPTS -f core-integration-testing-support/1.3/pom.xml deploy
mvn $OPTS -f core-integration-testing-support/1.4/pom.xml deploy

# Remove all integration testing artifacts that were installed in the local repository as many tests
# download and verify the download.
#rm -rf /tmp/mits/repository-local/org/apache/maven/its

# Now run the integration tests
#mvn $OPTS deploy
