#!/bin/bash

dest=/home/projects/maven/repository-staging/to-ibiblio

#-a = -rlptgoD
rsync -e ssh -v -rt login.ibiblio.org:/public/html/maven/ $dest/maven
rsync -e ssh -v -rt login.ibiblio.org:/public/html/maven2/ $dest/maven2

find -user $USER | xargs chmod g+rw
