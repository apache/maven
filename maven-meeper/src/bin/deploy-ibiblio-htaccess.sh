#!/bin/sh

if [ "`hostname`" == "beaver.codehaus.org" ]; then
  cp ibiblio-htaccess $HOME/maven/repository-staging/to-ibiblio/maven/.htaccess
else
  scp ibiblio-htaccess maven@beaver.codehaus.org:$HOME/maven/repository-staging/to-ibiblio/maven/.htaccess
fi

scp ibiblio-htaccess maven@login.ibiblio.org:/public/html/maven/.htaccess

