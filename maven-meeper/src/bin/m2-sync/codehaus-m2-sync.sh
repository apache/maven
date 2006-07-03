#!/bin/sh

FROM=mavensync@repository.codehaus.org:/var/www/domains/codehaus.org/repository/htdocs/org/codehaus/
TO=org/codehaus/
SSH_OPTS="-i $HOME/.ssh/new-id_dsa"
RSYNC_OPTS="-k"

# NOTE: If the rsync options change, the codehaus configuration may need to be changed.
# It currently runs "rsync --server --sender -vnlkogDtprcz . /var/www/domains/codehaus.org/repository/htdocs/org/codehaus/" regardless of the rsync command sent

source ./m2-sync.sh
