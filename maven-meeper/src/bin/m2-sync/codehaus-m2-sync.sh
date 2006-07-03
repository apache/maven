#!/bin/sh

FROM=mavensync@repository.codehaus.org:/var/www/domains/codehaus.org/repository/htdocs/org/codehaus/
TO=org/codehaus/
SSH_OPTS="-i $HOME/.ssh/new-id_dsa"
RSYNC_OPTS="-k"

source ./m2-sync.sh
