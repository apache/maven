#!/bin/sh

for i in `ls Maven*.java`
do
name=`echo $i | sed 's/Maven//' | sed 's/Test.java//' | tr A-Z a-z`
sed "s/core-it:touch/org.apache.maven.its.plugins:maven-it-plugin-touch:touch/" $i > $i.new
#sed "s/verifier.assertArtifactPresent( \"org.apache.maven\", \"maven-it-it/verifier.assertArtifactPresent( \"org.apache.maven.its.$name\", \"maven-it-it/" $i > $i.new
mv $i.new $i
done
