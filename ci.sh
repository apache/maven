#!/bin/sh

# ----------------------------------------------------------------------------------
# To run this script on your machine you must:
#
# 1. export JAVA_HOME=/path/to/java
#
# 2. export MBOOT_HOME=~/mboot
#
# 3. export MAVEN_HOME=~/maven
#    
# 4. export PATH=$PATH:$MBOOT:$MAVEN_HOME/bin
#
# 5. Your ~/build.properties must have: maven.repo.local = ~/path/to/repo
#
# ----------------------------------------------------------------------------------

export JAVA_HOME=/usr/local/java
export MBOOT_HOME=$HOME/mboot
export MAVEN_HOME=$HOME/maven
export PATH=$PATH:$MBOOT_HOME:$MAVEN_HOME/bin:$JAVA_HOME/bin

# ----------------------------------------------------------------------------------

CMD=$1

[ "$1" = "" ] && echo && echo "You must specify a checkout or update!" && echo && exit

DIR=maven2
REPO=maven-repo-local
FROM=jvanzyl@maven.org
TO=maven2-user@lists.codehaus.org
SCM_LOG=scm.log

# ----------------------------------------------------------------------------------

# Wipe out the working directory and the repository and start entirely
# from scratch.

# ----------------------------------------------------------------------------------

HOME_DIR=`pwd`
DATE=`date`
echo "From: $FROM" > log
echo "To: $TO" >> log
echo "Subject: Maven bootstrap on beaver [$DATE]" >> log
echo "" >> log

export CVSROOT=:pserver:anoncvs@cvs.apache.org:/home/cvspublic

(
  if [ "$CMD" = "checkout" ]
  then

    rm -rf $DIR > /dev/null 2>&1
      
    mkdir $DIR
      
    rm -rf $REPO > /dev/null 2>&1
      
    mkdir $REPO

    echo
    echo "Performing a clean check out of maven-components ..."
    echo

    (
      cd $DIR
        
      cvs co maven-components > $HOME_DIR/$SCM_LOG 2>&1
    
      echo "true" > $HOME_DIR/build_required     
    )
    
  else
    
    echo
    echo "Performing an update of maven-components ..."
    echo
      
    (
      cd $DIR/maven-components
      
      cvs update -dP > $HOME_DIR/$SCM_LOG 2>&1
      
      grep ^P $HOME_DIR/$SCM_LOG > /dev/null 2>&1

      if [ "$?" = "1" ]
      then
        
	echo "false" > $HOME_DIR/build_required
      
        else
	
	echo "true" > $HOME_DIR/build_required
	  
      fi

    )

  fi
    
  BUILD_REQUIRED=`cat $HOME_DIR/build_required`

  if [ "$BUILD_REQUIRED" = "true" ]
  then
      
    echo "Updates occured, build required ..."
    echo
    grep ^P $HOME_DIR/$SCM_LOG
    echo

    (
      cd $DIR/maven-components/maven-core
  
      ./bootstrap-all.sh
    )
  
  else
  
    echo "No updates occured, no build required. Done."
  
  fi

) >> log 2>&1

/usr/sbin/sendmail -t < log
