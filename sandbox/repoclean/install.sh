#!/bin/sh

if [ $# -eq 0 ]; then

    echo ""
    echo "*******************"
    echo "* Usage: 'sh ./install.sh /path/to/install/target /path/to/local/repo'"
    echo "*******************"
    echo ""
    
    exit 0
fi
    
(
    # First, ensure that the repoclean library has been built.
    
    echo "-----------------------------------------------------------------------"
    echo " Building a clean copy of repoclean ... "
    echo "-----------------------------------------------------------------------"  
    m2 clean:clean package
    ret=$?; if [ $ret != 0 ]; then exit $ret; fi
)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

(
    # Now, install the bash script and dependencies to the specified target dir.

    echo ""
    echo "-----------------------------------------------------------------------"
    echo " Installing repoclean to ${1} ... "
    echo "-----------------------------------------------------------------------"  
    echo ""

    mkdir -p $1/lib

    echo "Copying application libraries..."
    echo ""
    
    cp -f target/repoclean-1.0-SNAPSHOT.jar $1/lib

    version=`cat $2/plexus/plexus-container-default/1.0-alpha-3-SNAPSHOT/plexus-container-default-1.0-alpha-3-SNAPSHOT.version.txt`

    cp -f $2/plexus/plexus-container-default/1.0-alpha-3-SNAPSHOT/plexus-container-default-$version.jar $1/lib
    version="ERROR-IN-SCRIPT"

    version=`cat $2/plexus/plexus-mail-sender-api/1.0-alpha-1-SNAPSHOT/plexus-mail-sender-api-1.0-alpha-1-SNAPSHOT.version.txt`
    cp -f $2/plexus/plexus-mail-sender-api/1.0-alpha-1-SNAPSHOT/plexus-mail-sender-api-$version.jar $1/lib
    version="ERROR-IN-SCRIPT"

    version=`cat $2/plexus/plexus-mail-sender-simple/1.0-alpha-1-SNAPSHOT/plexus-mail-sender-simple-1.0-alpha-1-SNAPSHOT.version.txt`
    cp -f $2/plexus/plexus-mail-sender-simple/1.0-alpha-1-SNAPSHOT/plexus-mail-sender-simple-$version.jar $1/lib
    version="ERROR-IN-SCRIPT"

    cp -f $2/classworlds/classworlds/1.1-alpha-1/classworlds-1.1-alpha-1.jar $1/lib

    version=`cat $2/org/apache/maven/maven-artifact/2.0-SNAPSHOT/maven-artifact-2.0-SNAPSHOT.version.txt`
    cp -f $2/org/apache/maven/maven-artifact/2.0-SNAPSHOT/maven-artifact-$version.jar $1/lib
    version="ERROR-IN-SCRIPT"

    version=`cat $2/org/apache/maven/maven-model/2.0-SNAPSHOT/maven-model-2.0-SNAPSHOT.version.txt`
    cp -f $2/org/apache/maven/maven-model/2.0-SNAPSHOT/maven-model-$version.jar $1/lib
    version="ERROR-IN-SCRIPT"

    cp -f $2/org/apache/maven/wagon/wagon-provider-api/1.0-alpha-2/wagon-provider-api-1.0-alpha-2.jar $1/lib

    cp -f $2/org/apache/maven/wagon/wagon-file/1.0-alpha-2/wagon-file-1.0-alpha-2.jar $1/lib

    echo "Copying startup script, and changing its permissions to '+x'..."
    echo ""
    
    cp -f src/main/bash/repoclean.sh $1
    chmod +x $1/repoclean.sh
    
    ret=$?; if [ $ret != 0 ]; then exit $ret; fi

)
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

echo ""
echo "*******************"
echo "* repoclean utility has been installed to: ${1}."
echo "*"
echo "* To run, change to '${1}' and execute './repoclean.sh', which will give further usage instructions."
echo "*******************"
echo ""
