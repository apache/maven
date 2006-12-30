#!/bin/sh

# We will model what we want here and then get it to work in Continuum

# Problems
# maven-verifier 1.0-SNAPSHOT is not installed

# We want to do a bootstrap with a
# * clean checkout
# * clean repository

# To make sure that everything works starting from scratch

# * Keep track of everything that was used to build
# * Create a new user
# * Checkout and build versus taking the binary and running the ITs
# * Make sure SVN is installed

# Assumptions
# Ant is installed >1.6.5
# Subversion is installed

# Need to override the local repo
# Need a command line option for this

# 2.1-SNAPSHOT https://svn.apache.org/repos/asf/maven/components/trunk ant
# 2.0.5-SNAPSHOT https://svn.apache.org/repos/asf/maven/components/branches/maven-2.0.x bootstrap.sh 
# 2.0.4 https://svn.apache.org/repos/asf/maven/components/tags/maven-2.0.4 bootstrap.sh 

buildDirectory=/tmp/maven

mavenVersion=maven-2.1-SNAPSHOT
mavenHome=$buildDirectory/$mavenVersion
settings=$buildDirectory/settings.xml
maven="$mavenHome/bin/mvn -s $settings"

export M2_HOME=$mavenHome

echo "<?xml version="1.0"?>" > $settings
echo "<settings>" >> $settings
echo "  <localRepository>/tmp/maven/repository</localRepository>" >> $settings
echo "</settings>" >> $settings

echo "   Maven version: $mavenVersion"
echo "Maven executable: $maven"

rm -rf $buildDirectory > /dev/null 2>&1
mkdir -p $buildDirectory

( 
  cd $buildDirectory
  svn co https://svn.apache.org/repos/asf/maven/components/trunk maven-trunk
  svn co https://svn.apache.org/repos/asf/maven/components/branches/2.0.x maven-2.0.x  
  svn co https://svn.apache.org/repos/asf/maven/core-integration-testing/trunk mits-trunk
  
  # Build the selected version of Maven
  ( 
    cd maven-trunk
    ant
  )  

  mavenVersionString=`$maven -v`
  echo "Maven version: $mavenVersionString (verify)" 

  # Build the Maven Integration Tests
  (
    cd mits-trunk
    $maven install
    (
      cd core-integration-tests
      $maven test
    )
  )
)
