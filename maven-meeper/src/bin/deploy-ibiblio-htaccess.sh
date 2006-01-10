#!/bin/sh

if [ "`hostname`" == "beaver.codehaus.org" ]; then
  cp ibiblio-htaccess /home/projects/maven/repository-staging/to-ibiblio/maven/.htaccess
else
  scp ibiblio-htaccess maven@beaver.codehaus.org:/home/projects/maven/repository-staging/to-ibiblio/maven/.htaccess
fi

scp ibiblio-htaccess maven@login.ibiblio.org:/public/html/maven/.htaccess

