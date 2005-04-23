#!/bin/bash

copyFile()
{
  echo Copying:
  echo "    $1"
  echo "      --> $2"
  cp $1 $2

  if [ -f $1.sha1 ]; then
    echo "  (with sha1)"
    cp $1.sha1 $2.sha1
  fi

  if [ -f $1.md5 ]; then
    echo "  (with md5)"
    cp $1.md5 $2.md5
  fi
}

processPom()
{
  contents=`cat $1 | tr '\n' ' ' | sed 's#<parent>.*</parent>##m' | sed 's#<dependencies>.*</dependencies>##m'`
  groupId=`echo $contents | grep '<groupId>' | sed 's#^.*<groupId>##' | sed 's#</groupId>.*$##'`
  artifactId=`echo $contents | grep '<artifactId>' | sed 's#^.*<artifactId>##' | sed 's#</artifactId>.*$##'`
  version=`echo $contents | grep '<version>' | sed 's#^.*<version>##' | sed 's#</version>.*$##'`
  parent=`cat $1 | tr '\n' ' ' | grep '<parent>' | sed 's#^.*<parent>##' | sed 's#</parent>.*$##'`

  if [ -z "$groupId" ]; then
    groupId=`echo $parent | grep '<groupId>' | sed 's#^.*<groupId>##' | sed 's#</groupId>.*$##'`
  fi

  if [ -z "$artifactId" ]; then
    artifactId=`echo $parent | grep '<artifactId>' | sed 's#^.*<artifactId>##' | sed 's#</artifactId>.*$##'`
  fi

  if [ -z "$version" ]; then
    version=`echo $parent | grep '<version>' | sed 's#^.*<version>##' | sed 's#</version>.*$##'`
  fi

  if [ -z "$version" ]; then
    echo no version
    exit 1
  fi

  if [ -z "$artifactId" ]; then
    echo no artifactId
    exit 1
  fi

  if [ -z "$groupId" ]; then
    echo no groupId
    exit 1
  fi

  slashedGroupId=`echo $groupId | sed 's#\.#/#g'`
  tsVersion=`echo $1 | sed "s#^.*/$artifactId-##" | sed 's#.pom$##'`

  oldPath=$slashedGroupId/$artifactId/$version/$artifactId-$tsVersion.pom
  newPath=$slashedGroupId/$artifactId-$tsVersion.pom
  oldTxtVersion=$slashedGroupId/$artifactId/$version/$artifactId-$version.version.txt
  newTxtVersion=$slashedGroupId/$artifactId-$version.version.txt

  banner=0

  if [ ! -f $oldPath ]; then
    copyFile $1 $oldPath
    banner=1
  fi

  if [ ! -f $newPath ]; then
    copyFile $1 $newPath
    banner=1
  fi

  if [ -f $newTxtVersion ]; then
    if [ ! -f $oldTxtVersion ]; then
      copyFile $newTxtVersion $oldTxtVersion
      banner=1
    fi
  fi

  if [ -f $oldTxtVersion ]; then
    if [ ! -f $newTxtVersion ]; then
      copyFile $oldTxtVersion $newTxtVersion
      banner=1
    fi
  fi

  if [ $banner -eq 1 ]; then
    echo ==================================================
  fi
}

find . -mtime -1 -name '*.pom' | xargs grep -l '<packaging>pom</packaging>' | while read pom
do
  processPom $pom
done
