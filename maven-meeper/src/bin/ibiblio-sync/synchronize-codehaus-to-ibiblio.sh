#!/bin/bash

dest=/home/maven/repository-staging/to-ibiblio

rsync -e ssh --delete --max-delete=10 -v -riplt $dest/maven2/ login.ibiblio.org:/public/html/maven2

date > $dest/maven2/last-sync.txt
chmod a+r $dest/maven2/last-sync.txt


# NO MORE m1 SYNC

#date > $dest/maven/last-sync.txt
#chmod a+r $dest/maven/last-sync.txt
#rsync -e ssh --delete --max-delete=10 -v -riplt $dest/maven/ login.ibiblio.org:/public/html/maven

# EXCEPT FOR PLUGINS
# TODO - FIX
rsync -e ssh --delete --max-delete=10 -v -riplt $dest/maven/ login.ibiblio.org:/public/html/maven
#(
  #cd $dest/maven
  #rsync -n -e ssh --delete --max-delete=10 -v -riplt --include='*/plugins/*' ./ login.ibiblio.org:/public/html/maven
  #for i in */plugins
  #do
    #rsync -e ssh --delete --max-delete=10 -v -riplt $i login.ibiblio.org:/public/html/maven/$i
  #done
#)
