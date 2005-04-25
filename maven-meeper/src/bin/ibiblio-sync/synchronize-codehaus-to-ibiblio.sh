#!/bin/bash

dest=/home/projects/maven/repository-staging/to-ibiblio

date > $dest/maven/last-sync.txt
rsync -e ssh --exclude='*/distributions/*' -v -rlt $dest/maven/ login.ibiblio.org:/public/html/maven

date > $dest/maven2/last-sync.txt
rsync -e ssh --exclude='*/distributions/*' -v -rlt $dest/maven2/ login.ibiblio.org:/public/html/maven2
