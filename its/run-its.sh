#!/bin/sh

# How I run the ITs from a clean slate. Though I do this with a primed Nexus instance. JvZ.

mvn clean install -Prun-its,embedded -Dmaven.repo.local=`pwd`/repo

# If behind a proxy try this

# mvn clean install -Prun-its,embedded -Dmaven.repo.local=`pwd`/repo -Dproxy.active=true -Dproxy.type=http -Dproxy.host=<host> -Dproxy.port=<port> -Dproxy.user= -Dproxy.pass=
