#!/bin/sh

[ -z "$LOCAL_REPO" ] && echo && echo "Set your LOCAL_REPO envar!" && echo && exit

echo "Using $LOCAL_REPO ..."

mv "${LOCAL_REPO}" "${LOCAL_REPO}.its"

mvn -N install \
&& ( 
  cd core-integration-testing-plugins
  mvn install
  ret=$?; if [ $ret != 0 ]; then echo "Failed to install IT plugins" && exit $ret; fi
) && ( 
  cd core-integration-testing-support
  mvn install 
  ret=$?; if [ $ret != 0 ]; then echo "Failed to install IT support artifacts." && exit $ret; fi
) && ( 
  cd core-integration-tests
  mvn clean test
  ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?

rm -rf "${LOCAL_REPO}"
mv "${LOCAL_REPO}.its" "${LOCAL_REPO}"

exit $ret
