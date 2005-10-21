Installing Maven 2
==================

The following instructions show how to install Maven 2:

1) Unpack the archive where you would like to store the binaries, eg:
  tar zxvf maven-2.0.tar.gz
or
  unzip maven-2.0.zip

2) A directory called "maven-2.0" will be created.

3) Add the bin directory to your PATH, eg:
  export PATH=/usr/local/maven-2.0/bin:$PATH
or
  set PATH="c:\program files\maven-2.0\bin";%PATH%

4) Make sure JAVA_HOME is set to the location of your JDK

5) Run "mvn --version" to verify that it is correctly installed.

For more information, please see http://maven.apache.org

