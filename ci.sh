#!/bin/sh

# ----------------------------------------------------------------------------------

. $HOME/.profile
cd $HOME

CMD=$1

[ "$1" = "" ] && echo && echo "You must specify a checkout or update!" && echo && exit 1

FROM=continuum@maven.zones.apache.org
TO=dev@maven.apache.org
DATE=`date`

PID=$$
RUNNING=`ps -ef | grep ci.sh | grep -v 'sh -c' | grep -v grep | grep -v $PID`
if [ ! -z "$RUNNING" ]; then
  if [ "$CMD" = "checkout" ]; then
    echo "From: $FROM" > running_log
    echo "To: $TO" >> running_log
    echo "Subject: [maven2 build - SKIPPED - $CMD] $DATE" >>running_log
    echo "" >> running_log
    echo "ci.sh already running... exiting" >>running_log
    echo "$RUNNING" >>running_log
    /usr/sbin/sendmail -t < running_log
  fi
  exit 1
fi

HOME_DIR=`pwd`
DIR=$HOME/m2-build
REPO=$HOME_DIR/maven-repo-local
SCM_LOG=scm.log
TIMESTAMP=`date +%Y%m%d.%H%M%S`
WWW=$HOME/public_html
DEPLOY_DIR=$WWW/builds
DEPLOY_SITE=http://maven.zones.apache.org/~maven/builds
DIST=m2-${TIMESTAMP}.tar.gz
SVN=svn

M2_HOME=$HOME/m2
export M2_HOME
PATH=$PATH:$JAVA_HOME/bin:$M2_HOME/bin
export PATH

MESSAGE_DIR=$WWW/logs
MESSAGE_NAME=m2-build-log-${TIMESTAMP}.txt
MESSAGE=${MESSAGE_DIR}/${MESSAGE_NAME}

mkdir -p $DEPLOY_DIR
mkdir -p $MESSAGE_DIR

# ----------------------------------------------------------------------------------

# Wipe out the working directory and the repository and start entirely
# from scratch.

# ----------------------------------------------------------------------------------

BUILD_REQUIRED=false
if [ -f $HOME_DIR/build_required ]; then
  BUILD_REQUIRED=`cat $HOME_DIR/build_required`
fi

if [ ! -d $DIR/maven-components ]; then
  CMD="checkout"
fi

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
        
      $SVN co http://svn.apache.org/repos/asf/maven/components/trunk maven-components > $HOME_DIR/$SCM_LOG 2>&1
    
      echo "true" > $HOME_DIR/build_required     
    )
    
  else
    
    echo
    echo "Performing an update of maven-components ..."
    echo
      
    (
      cd $DIR/maven-components
      
      $SVN update > $HOME_DIR/$SCM_LOG 2>&1
      
      grep "^[PUAD] " $HOME_DIR/$SCM_LOG > /dev/null 2>&1

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
    grep "^[PUAD] " $HOME_DIR/$SCM_LOG
    echo

    (
      cd $DIR/maven-components
  
      sh m2-bootstrap-all.sh -Dmaven.repo.local="$REPO" -Dmaven.home="$M2_HOME" --update-snapshots
      ret=$?; if [ $ret != 0 ]; then exit $ret; fi
    )    
    ret=$?; if [ $ret != 0 ]; then exit $ret; fi

    # Only created on success

    echo
    echo "Creating m2 distribution for public consumption: ${DEPLOY_SITE}/${DIST}"
    echo

    mkdir -p $DEPLOY_DIR > /dev/null 2>&1

    # Assumes pwd is still $HOME_DIR
    gtar czf $DEPLOY_DIR/$DIST m2

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
    echo "Subject: [maven2 build - FAILED - $CMD] $DATE" >> log
  else
    echo "Subject: [maven2 build - SUCCESS - $CMD] $DATE" >> log
    echo "" >> log
    echo "Distribution:" >> log
    echo "${DEPLOY_SITE}/${DIST}" >>log
    rm $HOME_DIR/build_required
  fi
  echo "" >> log
  echo "Log:" >> log
  echo "http://maven.zones.apache.org/~maven/logs/${MESSAGE_NAME}" >> log

  /usr/sbin/sendmail -t < log
fi
