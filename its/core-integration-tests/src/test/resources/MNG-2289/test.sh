#!/bin/sh

# 1. Go to the parent directory and call mvn deploy
# 2. Go to the issue directory and call mvn eclipse:eclipse, the project is now dependent on junit-3.7 as defined in the dependencyManagement of the parent
# 3. Set your local repo in settings.xml to something else e.g. <localRepository>/temp/repo-m2-local</localRepository>
# 4. Go to the parent directory and change the version of junit to 3.8
# 5. Call again mvn deploy, but Maven will now use the temporary local repo for installation of the updated POM
# 6. Go to the issue directory and call mvn eclipse:eclipse, the project is now dependent on junit-3.8 as defined in the dependencyManagement of the parent
# 7. Restore your settings.xml
# 8. Call mvn eclipse:eclipse again for the issue, the project is still dependent on junit-3.7 the available newer version of the snapshot parent is ignored

# We want to test that after changing the parent POM which is a SNAPSHOT, it is used for any children.

dir=`pwd`

mvn="mvn"
repo="$HOME/maven-repo-local"

rm -rf $repo/commons-logging
( cd parent ; cp pom-with-ju-3.7.xml pom.xml ; $mvn deploy )
( cd issue; $mvn compile )
( cd parent ; cp pom-with-ju-3.8.xml pom.xml ; $mvn deploy )
mv $repo/commons-logging $repo/commons-logging-1.0.1
( cd issue; $mvn compile )

