#!/bin/sh

if [ "`hostname`" == "maven01.managed.contegix.com" ]; then
  cp ibiblio-htaccess $HOME/repository-staging/to-ibiblio/maven/.htaccess
else
  scp ibiblio-htaccess maven@maven.org:~maven/repository-staging/to-ibiblio/maven/.htaccess
fi

scp ibiblio-htaccess maven@login.ibiblio.org:/public/html/maven/.htaccess

