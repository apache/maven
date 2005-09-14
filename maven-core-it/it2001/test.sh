#!/bin/bash

echo "Clearing out residual working directories"
rm -Rf `find . -type d -name target`

echo "Deploying 'c'"
(cd c && m2 --settings ../settings.xml deploy)

echo "Installing 'b'"
(cd b && m2 --settings ../settings.xml install)

echo "Installing 'a'"
(cd a && m2 --settings ../settings-norepo.xml install)

echo "Removing 'c' from local repository"
rm -Rf target/local-repository/org/apache/maven/it2001/c

echo "Re-running 'a' install"
(cd a && m2 --settings ../settings-norepo.xml install)

echo "Cleaning up."
rm -Rf `find . -type d -name target`

