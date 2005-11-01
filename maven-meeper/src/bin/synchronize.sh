#!/bin/sh

PID=$$
RUNNING=`ps -ef | grep synchronize.sh | grep -v 'sh -c' | grep -v grep | grep -v $PID`
if [ ! -z "$RUNNING" ]; then
  echo Sync already running... exiting
  echo $RUNNING
  exit 1
fi


TOOLS_BASE=/home/projects/maven/repository-tools
(
  cd $TOOLS_BASE/syncopate
  ./sync
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi

(
  cd $TOOLS_BASE/repoclean
  ./repoclean.sh synchronize.properties
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi


# get poms from svn and generate checksums
(
  src=/home/projects/maven/repository-staging/pom-svn-repository
  dest=/home/projects/maven/repository-staging/to-ibiblio/maven2
  cd $src
  /usr/local/subversion/bin/svn update
  rsync -e ssh -v -rpt --exclude=.svn --exclude=updated-poms.log $src/ $dest/ > updated-poms.log
  for f in `grep .pom updated-poms.log` ; do md5sum $dest/$f > $dest/$f.md5 ; sha1sum $dest/$f > $dest/$f.sha1; done
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi


(
  cd $TOOLS_BASE/ibiblio-sync
  ./synchronize-codehaus-to-ibiblio.sh
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi

