#!/bin/sh

# To run this script on your machine you must set:
#
# MBOOT_HOME
# PATH=$PATH:$MBOOT_HOME/bin
# MAVEN_HOME
# PATH=$PATH:$MAVEN_HOME/bin

DIR=maven2
REPO=maven-repo-local

rm -rf $DIR > /dev/null 2>&1
rm -rf $REPO > /dev/null 2>&1
mkdir $DIR
mkdir $REPO

DATE=`date`
echo "From: jvanzyl@maven.org" > log
echo "To: maven2-user@lists.codehaus.org" >> log
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
