#!/bin/sh

# ----------------------------------------------------------------------------------
# To run this script on your machine you must:
#
# 1. export MBOOT_HOME=~/mboot
#
# 2. export MAVEN_HOME=~/maven
#    
# 3. export PATH=$PATH:$MBOOT:$MAVEN_HOME/bin
#
# 4. Your ~/build.properties must have: maven.repo.local = ~/path/to/repo
#
# ----------------------------------------------------------------------------------

DIR=maven2
REPO=maven-repo-local
FROM=jvanzyl@maven.org
TO=maven2-user@lists.codehaus.org

# ----------------------------------------------------------------------------------

# Wipe out the working directory and the repository and start entirely
# from scratch.

rm -rf $DIR > /dev/null 2>&1
rm -rf $REPO > /dev/null 2>&1
mkdir $DIR
mkdir $REPO

# ----------------------------------------------------------------------------------

DATE=`date`
echo "From: $FROM" > log
echo "To: $TO" >> log
echo "Subject: Maven bootstrap on beaver [$DATE]" >> log
echo "" >> log

(
  (
    cd $DIR

    export CVSROOT=:pserver:anoncvs@cvs.apache.org:/home/cvspublic

    echo "Checking out maven-components ..."

    cvs co maven-components > checkout.log 2>&1
  )

  (
    cd $DIR/maven-components/maven-core
  
    ./bootstrap-all.sh
  )  
) >> log 2>&1

sendmail -t < log
