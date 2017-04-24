# Maven Wrapper

The Maven Wrapper is an easy way to ensure a user of your Maven build has everything necessary to run your Maven build.
Why might this be necessary? Maven to date has been very stable for users, is available on most systems or is easy to
procure: but with many of the recent changes in Maven it will be easier for users to have a fully encapsulated build
setup provided by the project. With the Maven Wrapper this is very easy to do and it's a great idea borrowed from Gradle.

The easiest way to setup the Maven Wrapper for your project is to use the [Takari Maven Plugin][1] with its provided
`wrapper` goal. To add or update all the necessary Maven Wrapper files to your project execute the following command:

```
mvn -N io.takari:maven:wrapper
```

Normally you instruct users to run the `mvn` command like the following:

```
$ mvn clean install
```

But now, with a Maven Wrapper setup, you can instruct users to run wrapper scripts:

```
$ ./mvnw clean install
```

or

```
$ ./mvnw.cmd clean install
```

A normal Maven build will be executed with the one important change that if the user doesn't have the necessary version
of Maven specified in `.mvn/wrapper/maven-wrapper.properties` it will be downloaded for the user first.

## Supported Systems

The wrapper should work on various operating systems including

* Linux (numerous versions, tested on Ubuntu and CentOS)
* OSX
* Windows (various newer versions)
* Solaris (10 and 11)

For all those *nix operating systems, various shells should work including

* sh
* bash
* dash
* zsh

In terms of Apache Maven versions itself, the wrapper should work with any Maven 3.x version and it defaults to the
latest release - currently 3.5.0. We do NOT plan the deprecated, EOL'ed Maven 2.x.

The maven-wrapper itself is compiled to work with Java 5. The Takari Maven Plugin for installation however uses Java 7.
Once the wrapper is installed with the plugin you should be able to use the wrapper on the project with Java 5 and up.
This is however not really tested by the committers.

## Changes

Please check out the [changelog](./CHANGELOG.md) for more information about our releases.
 
## Using a Different Version of Maven

To switch the version of Maven used to build a project you can initialize it using 

```
mvn -N io.takari:maven:wrapper -Dmaven=3.3.3
```

which works for any version except snapshots. Once you have a wrapper you can change its version by setting the
`distributionUrl` in `.mvn/wrapper/maven-wrapper.properties`, e.g.

```
distributionUrl=https://repo1.maven.org/maven2/org/apache/maven/apache-maven/3.2.1/apache-maven-3.2.1-bin.zip
```

[1]: https://github.com/takari/takari-maven-plugin
