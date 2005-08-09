#!/bin/bash

echo "Cleaning Test Repository"

rm -Rf test-repo

echo "Building Plugin"

cd plugin

m2 -DupdateReleaseInfo=true clean:clean deploy

echo "Building Project"

cd ../project

m2 --settings ./settings.xml --no-plugin-registry --check-plugin-latest it2000:test
