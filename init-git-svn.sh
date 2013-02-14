cd .git;wget http://git.apache.org/authors.txt; cd ..
git config svn.authorsfile ".git/authors.txt"
git svn init --prefix=origin/ --tags=tags --trunk=trunk --branches=branches https://svn.apache.org/repos/asf/maven/maven-3
git svn rebase
