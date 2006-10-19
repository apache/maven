#!/bin/sh

for i in `ls -1`
do
  svn add $i
  svn commit -m "o adding IT $i" $i
done
