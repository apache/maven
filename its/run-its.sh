#!/bin/sh

# How I run the ITs from a clean slate. Though I do this with a primed Nexus instance. JvZ.

mvn clean install -Prun-its,embedded -Dmaven.repo.local=`pwd`/repo