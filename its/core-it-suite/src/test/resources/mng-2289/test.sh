#!/bin/sh

# We want to test that after changing the parent POM which is a SNAPSHOT, it is used for any children.

dir=`pwd`

mvn="mvn"
repo="$HOME/maven-repo-local"

# Remove commons-logging all together
rm -rf $repo/commons-logging
# Deploy the parent POM in our file-based remote repository
( cd parent ; $mvn -f pom1.xml deploy )
# Run the compile phase for the child project. This will bring down commons-logging 1.0.1
( cd issue; $mvn compile )
# Deploy the parent POM with an update version of the commons-logging dependency -> 1.0.2

read

( cd parent ; $mvn -f pom2.xml deploy )
# Move the original commons-logging deps out of the way
mv $repo/commons-logging $repo/commons-logging-1.0.1
# Run the child project again and the new version of commons-logging should come down
( cd issue; $mvn compile )

