#!/bin/sh

FROM=rsync://databinder.net/maven/net/databinder/
TO=net/databinder/

NO_SSH=true

source ./m2-sync.sh
