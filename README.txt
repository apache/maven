-------------------------------------------------------------------------------
Bootstrapping Maven
-------------------------------------------------------------------------------

Set the environment variable M2_HOME pointing to the dir where you want Maven2 installed.

NOTE: presently, the directory {M2_HOME}/bin must be in your path:
set PATH=%PATH%;%M2_HOME%\bin
or
export PATH=$PATH:$M2_HOME/bin

You can set the parameters passed to the Java VM when running Maven2 bootstrap,
setting the environment variable MAVEN_OPTS, e.g.
e.g. to run in offline mode, set MAVEN_OPTS=-o

Then run m2-bootstrap-all.bat (in Windows) or m2-bootstrap-all.sh (in Unix)

NOTE: You must run these instructions from this directory!
