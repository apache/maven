& $JAVACMD `
  $MAVEN_OPTS `
  $MAVEN_DEBUG_OPTS `
  -classpath $LAUNCHER_JAR `
  "-Dclassworlds.conf=$CLASSWORLDS_CONF" `
  "-Dmaven.home=$env:MAVEN_HOME" `
  "-Dlibrary.jansi.path=$env:MAVEN_HOME\lib\jansi-native" `
  "-Dmaven.multiModuleProjectDirectory=$basedir" `
  $LAUNCHER_CLASS `
  $env:MAVEN_ARGS `
  $args


