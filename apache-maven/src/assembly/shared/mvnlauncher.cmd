for %%i in ("%MAVEN_HOME%"\boot\plexus-classworlds-*) do set LAUNCHER_JAR="%%i"
set MAVEN_LAUNCHER=org.codehaus.plexus.classworlds.launcher.Launcher
set MAVENHOME_CONFIG="-Dclassworlds.conf=%MAVEN_HOME%\bin\m2.conf" "-Dmaven.home=%MAVEN_HOME%"

