#!/bin/sh

# ----------------------------------------------------------------------------------

. $HOME/.profile
cd $HOME

CMD=$1

if [ "$1" = "" ]; then
  echo
  echo "You must specify a checkout or update!"
  echo
  exit 1
fi

BRANCH="branches/${2}"

if [ "$2" = "" ]; then
  BRANCH="trunk"
fi

FROM=continuum@maven.zones.apache.org
TO=notifications@maven.apache.org
DATE=`date`

PID=$$
RUNNING=`ps -ef | grep ci.sh | grep -v 'sh -c' | grep -v grep | grep -v $PID`
if [ ! -z "$RUNNING" ]; then
  if [ "$CMD" = "checkout" ]; then
    echo "From: $FROM" > running_log
    echo "To: $TO" >> running_log
    echo "Subject: [maven2 build $BRANCH - SKIPPED - $CMD] $DATE" >>running_log
    echo "" >> running_log
    echo "ci.sh already running... exiting" >>running_log
    echo "$RUNNING" >>running_log
    /usr/sbin/sendmail -t < running_log
  fi
  exit 1
fi

DIR=$HOME/m2-build/$BRANCH
mkdir -p $DIR

REPO=$DIR/maven-repo-local
# TODO: not good for concurrency - need to pass in to bootstrap
cat $HOME/.m2/settings.xml.template | sed "s#<localRepository>.*</localRepository>#<localRepository>$REPO</localRepository>#" >$HOME/.m2/settings.xml

# Temporary to try and alleviate build failure due to race condition
rm -rf $REPO/org/apache/maven/plugins/maven-plugin-plugin

SCM_LOG=$DIR/scm.log
BUILD_REQUIRED_FILE=$DIR/build_required

TIMESTAMP=`date +%Y%m%d.%H%M%S`
WWW=$HOME/public_html
DEPLOY_DIR=$WWW/builds/$BRANCH
DEPLOY_SITE=http://maven.zones.apache.org/~maven/builds/$BRANCH
DIST=m2-${TIMESTAMP}.tar.gz

SVN=svn
SED=gsed

MESSAGE_DIR=$WWW/logs/$BRANCH
MESSAGE_NAME=m2-build-log-${TIMESTAMP}.txt
MESSAGE_SITE=http://maven.zones.apache.org/~maven/logs/$BRANCH
MESSAGE=${MESSAGE_DIR}/${MESSAGE_NAME}

mkdir -p $DEPLOY_DIR
mkdir -p $MESSAGE_DIR

# ----------------------------------------------------------------------------------

# Wipe out the working directory and the repository and start entirely
# from scratch.

# ----------------------------------------------------------------------------------

BUILD_REQUIRED=false
if [ -f $BUILD_REQUIRED_FILE ]; then
  BUILD_REQUIRED=`cat $BUILD_REQUIRED_FILE`
fi

if [ ! -d $DIR/maven-components ]; then
  CMD="checkout"
fi

if [ ! -d $DIR/plugins ]; then
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
        
      $SVN co http://svn.apache.org/repos/asf/maven/components/$BRANCH maven-components > $SCM_LOG 2>&1
      $SVN co http://svn.apache.org/repos/asf/maven/plugins/trunk plugins > $SCM_LOG 2>&1
    
      echo "true" > $BUILD_REQUIRED_FILE
    )
    
  else
    
    echo
    echo "Performing an update of maven-components ..."
    echo
      
    (
      cd $DIR/maven-components
      
      $SVN update > $SCM_LOG 2>&1

      cd $DIR/plugins

      $SVN update >> $SCM_LOG 2>&1
      
      grep "^[PUAD] " $SCM_LOG > /dev/null 2>&1

      if [ "$?" = "1" ]
      then
        
	echo $BUILD_REQUIRED > $BUILD_REQUIRED_FILE
      
        else
	
	echo "true" > $BUILD_REQUIRED_FILE
	  
      fi

    )

  fi
    
  BUILD_REQUIRED=`cat $BUILD_REQUIRED_FILE`

  if [ "$BUILD_REQUIRED" = "true" ]
  then
      
    echo "Updates occured, build required ..."
    echo
    grep "^[PUAD] " $SCM_LOG
    echo

    version=`cat $DIR/maven-components/pom.xml | tr '\n' ' ' | $SED 's#<parent>.*</parent>##g' | $SED 's#<dependencies>.*</dependencies>##g' | $SED 's#<build>.*</build>##g' | $SED 's#^.*<version>##g' | $SED 's#</version>.*$##g'`

    M2_HOME=$DIR/maven-$version
    export M2_HOME
    PATH=$JAVA_HOME/bin:$M2_HOME/bin:$PATH
    export PATH

    (
      cd $DIR/maven-components
  
      sh bootstrap.sh --prefix=$DIR --update-snapshots
      ret=$?; if [ $ret != 0 ]; then exit $ret; fi
    )    
    ret=$?; if [ $ret != 0 ]; then exit $ret; fi

    # Only created on success

    echo
    echo "Creating Maven distribution for public consumption: ${DEPLOY_SITE}/${DIST}"
    echo

    mkdir -p $DEPLOY_DIR > /dev/null 2>&1

    cp $DIR/maven-components/maven-core/target/*.tar.gz $DEPLOY_DIR/$DIST

  else
  
    echo "No updates occured, no build required. Done."
  
  fi

) >> $MESSAGE 2>&1
ret=$?

BUILD_REQUIRED=`cat $BUILD_REQUIRED_FILE`

# Only send mail to the list if a build was required.

host=`hostname`

if [ "$BUILD_REQUIRED" = "true" ]
then
  echo "From: $FROM" > log
  echo "To: $TO" >> log
  if [ $ret != 0 ]; then
    echo "Subject: [maven2 build $BRANCH - FAILED - $CMD] $DATE" >> log
  else
    echo "Subject: [maven2 build $BRANCH - SUCCESS - $CMD] $DATE" >> log
    echo "" >> log
    echo "Distribution:" >> log
    echo "${DEPLOY_SITE}/${DIST}" >>log
    rm $BUILD_REQUIRED_FILE
  fi
  echo "" >> log
  echo "Log:" >> log
  echo "${MESSAGE_SITE}/${MESSAGE_NAME}" >> log

  /usr/sbin/sendmail -t < log
fi

