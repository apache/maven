#!/bin/sh

# ----------------------------------------------------------------------------------
# To run this script on your machine you must:
#
# 1. export JAVA_HOME=/path/to/java
#
# 2. export MBOOT_HOME=~/mboot
#
# 3. export M2_HOME=~/maven
#    
# 4. export PATH=$PATH:$MBOOT:$M2_HOME/bin
#
# 5. Your ~/maven.properties must have: maven.repo.local = ~/path/to/repo
#
# ----------------------------------------------------------------------------------

export JAVA_HOME=/usr/local/java
export MBOOT_HOME=$HOME/mboot
export M2_HOME=$HOME/m2
export PATH=$PATH:$MBOOT_HOME:$M2_HOME/bin:$JAVA_HOME/bin

# ----------------------------------------------------------------------------------

CMD=$1

[ "$1" = "" ] && echo && echo "You must specify a checkout or update!" && echo && exit 1

HOME_DIR=`pwd`
DATE=`date`
DIR=m2-build
REPO=maven-repo-local
FROM=jvanzyl@maven.org
#TO=maven2-user@lists.codehaus.org
TO=m2-dev@maven.apache.org
SCM_LOG=scm.log
TIMESTAMP=`date +%Y%M%d.%H%M%S`
DEPLOY_DIR=$HOME_DIR/public_html/m2
DEPLOY_SITE=http://www.codehaus.org/~jvanzyl/m2

# ----------------------------------------------------------------------------------

# Wipe out the working directory and the repository and start entirely
# from scratch.

# ----------------------------------------------------------------------------------

echo "From: $FROM" > log
echo "To: $TO" >> log
echo "Subject: [maven2 build] $DATE" >> log
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
      cd $DIR/maven-components
  
      ./m2-bootstrap-all.sh
    )    

    DIST=m2-${TIMESTAMP}.tar.gz

    echo
    echo "Creating m2 distribution for public consumption: ${DEPLOY_SITE}/${DIST}"
    echo
    
    (
      cd $DIR/maven-components/maven-core/dist
      
      tar czf $DIST m2
      
      cp $DIST $DEPLOY_DIR
    )

  else
  
    echo "No updates occured, no build required. Done."
  
  fi

) >> log 2>&1

BUILD_REQUIRED=`cat $HOME_DIR/build_required`

# Only send mail to the list if a build was required.

host=`hostname`

if [ "$BUILD_REQUIRED" = "true" ]
then
  if [ "$hostname" = "beaver.codehaus.org" ]
  then
    /usr/sbin/sendmail -t < log
  fi  
fi
