Installing Maven 2
==================

The following instructions show how to install Maven 2:

1) Unpack the archive where you would like to store the binaries, eg:
  tar zxvf maven-2.0-SNAPSHOT.tar.gz
or
  unzip maven-2.0-SNAPSHOT.zip

2) A directory called "maven-2.0-SNAPSHOT" will be created.

3) Add the bin directory to your PATH, eg:
  export PATH=/usr/local/maven-2.0-SNAPSHOT/bin:$PATH
or
  set PATH="c:\program files\maven-2.0-SNAPSHOT\bin";%PATH%

4) Make sure JAVA_HOME is set to the location of your JDK

5) Run "m2" to verify that it is correctly installed. It should output a short build message, but do nothing


For more information, please see http://maven.apache.org/maven2/

