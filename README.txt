-------------------------------------------------------------------------------
Bootstrapping Maven
-------------------------------------------------------------------------------

To bootstrap Maven you must have a ~/.m2/maven.properties file with the following
entry:

maven.repo.local = /path/to/your/local/repository

Set the environment variable M2_HOME pointing to the dir where you want Maven2 installed.

NOTE: presently, the directory {M2_HOME}/bin must be in your path:
set PATH=%PATH%;%M2_HOME%\bin
or
export PATH=$PATH:$M2_HOME/bin

You can set the parameters passed to the Java VM when running Maven2 bootstrap,
setting the environment variable MAVEN_OPTS, e.g.
e.g. to run in offline mode, set MAVEN_OPTS=-Dmaven.online=false
e.g. to build maven with debug info, set MAVEN_OPTS=-Dmaven.compiler.debug=true

Then run m2-bootstrap-all.bat (in Windows) or m2-bootstrap-all.sh (in Unix)

NOTE: You must run these instructions from this directory!
