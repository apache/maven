# Maven

Maven is available under the [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)

- [Maven Issue Tracker](http://jira.codehaus.org/browse/MNG)
- [Maven Wiki](https://cwiki.apache.org/confluence/display/MAVEN/Index)
- [Building Maven](http://maven.apache.org/guides/development/guide-building-m2.html)

## Bootstrapping Basics

If you want to bootstrap Maven you'll need:

- Java 1.6+
- Ant 1.8 or later

First, give Ant a location into which the completed Maven distro should be installed:

```
export M2_HOME=$HOME/apps/maven/apache-maven-3.0-SNAPSHOT
```

Then, run Ant:

```
ant
```

Once the build completes, you should have a new Maven distro ready to roll in your $M2_HOME directory!

