#!/bin/sh

# ----------------------------------------------------------------------------------

CMD=$1

[ "$1" = "" ] && echo && echo "You must specify a checkout or update!" && echo && exit 1

HOME_DIR=`pwd`
DATE=`date`
DIR=m2-build
REPO=maven-repo-local
FROM=jvanzyl@maven.org
#FROM=brett@apache.org
#TO=maven2-user@lists.codehaus.org
TO=m2-dev@maven.apache.org
SCM_LOG=scm.log
TIMESTAMP=`date +%Y%m%d.%H%M%S`
DEPLOY_DIR=$HOME_DIR/public_html/m2
DEPLOY_SITE=http://www.codehaus.org/~maven/m2
DIST=m2-${TIMESTAMP}.tar.gz

export JAVA_HOME=/usr/local/java
# Required until classworlds.conf is updated
#export M2_HOME=$HOME_DIR/m2
export M2_HOME=$HOME/m2
export PATH=$PATH:$JAVA_HOME/bin:$M2_HOME/bin
export MESSAGE_DIR=$HOME_DIR/public_html/m2-build-logs
export MESSAGE_NAME=m2-build-log-${TIMESTAMP}.txt
export MESSAGE=${MESSAGE_DIR}/${MESSAGE_NAME}

# ----------------------------------------------------------------------------------

# Wipe out the working directory and the repository and start entirely
# from scratch.

# ----------------------------------------------------------------------------------

BUILD_REQUIRED=false
if [ -f $HOME_DIR/build_required ]; then
  BUILD_REQUIRED=`cat $HOME_DIR/build_required`
fi

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
        
	echo $BUILD_REQUIRED > $HOME_DIR/build_required
      
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
  
      sh m2-bootstrap-all.sh -Dmaven.repo.local="$HOME_DIR/$REPO" -Dmaven.home="$M2_HOME"
      ret=$?; if [ $ret != 0 ]; then exit $ret; fi
    )    
    ret=$?; if [ $ret != 0 ]; then exit $ret; fi

    # Only created on success

    echo
    echo "Creating m2 distribution for public consumption: ${DEPLOY_SITE}/${DIST}"
    echo

    mkdir -p $DEPLOY_DIR > /dev/null 2>&1

    # Assumes pwd is still $HOME_DIR
    tar czf $DEPLOY_DIR/$DIST m2

  else
  
    echo "No updates occured, no build required. Done."
  
  fi

) >> $MESSAGE 2>&1
ret=$?

BUILD_REQUIRED=`cat $HOME_DIR/build_required`

# Only send mail to the list if a build was required.

host=`hostname`

if [ "$BUILD_REQUIRED" = "true" ]
then
  echo "From: $FROM" > log
  echo "To: $TO" >> log
  if [ $ret != 0 ]; then
    echo "Subject: [maven2 build - FAILED] $DATE" >> log
  else
    echo "Subject: [maven2 build - SUCCESS] $DATE" >> log
    echo "" >> log
    echo "Distribution:" >> log
    echo "${DEPLOY_SITE}/${DIST}" >>log
    rm $HOME_DIR/build_required
  fi
  echo "" >> log
  echo "Log:" >> log
  echo "http://www.codehaus.org/~maven/m2-build-logs/${MESSAGE_NAME}" >> log

  /usr/sbin/sendmail -t < log
fi
