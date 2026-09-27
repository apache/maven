Apache Maven CLAPP (Command Line App) Directory
================================================

This directory hosts isolated classpath directories for extra Maven CLI tools
("CLAPPs") that do not want to bundle their private dependencies into the
shared ${maven.home}/lib/ directory.

Directory layout
----------------

  lib/
    clapp/
      <toolname>/
        clapp.properties     <- required descriptor
        *.jar                <- tool-private runtime jars (may be empty)

Each sub-directory corresponds to one CLAPP tool whose name matches the value
passed to `mvn --clapp <toolname>`.  All *.jar files inside that directory are
added to a child ClassLoader that delegates to the core Maven ClassLoader, so
the tool can use core Maven APIs while isolating its own dependencies.

clapp.properties format
-----------------------

  # Fully-qualified class name of the CLAPP entry point.
  # The class should expose:
  #   public static int run(String[] args, ClassWorld world)
  # (or public static int main(String[] args, ClassWorld world))
  mainClass=com.example.mytool.MyCling

Launching a CLAPP
-----------------

  mvn --clapp <toolname> [tool-specific arguments...]
  mvn --clapp=<toolname> [tool-specific arguments...]

The `mvn` script reads lib/clapp/<toolname>/clapp.properties, sets the
maven.clapp.name and maven.clapp.mainClass JVM system properties, and then
starts org.apache.maven.cling.MavenClappCling which delegates to the CLAPP's
main class inside the tool-specific ClassLoader.

Built-in tools (mvnenc, mvnsh, mvnup) continue to use their dedicated Cling
classes and do NOT require a clapp.properties file because they ship their
dependencies in the shared lib/ directory.

For comprehensive documentation on authoring third-party CLAPP tools, see the
Maven 4 API - CLI site documentation (clapp.html).
