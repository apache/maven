#!/bin/sh

v="1.0 1.1 1.1-old-location 1.2 1.3 1.4"

for i in $v
do
  ( cd $i; mvn install )
done
