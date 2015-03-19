# Maven

Maven is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)

- [Maven Issue Tracker](http://jira.codehaus.org/browse/MNG)
- [Maven Wiki](https://cwiki.apache.org/confluence/display/MAVEN/Index)
- [Building Maven](http://maven.apache.org/guides/development/guide-building-m2.html)
- [Running Core ITs](http://maven.apache.org/core-its/core-it-suite/)

## Bootstrapping Basics

If you want to bootstrap Maven, you'll need:

- Java 1.7+
- Ant 1.8 or later

Run Ant, specifying a location into which the completed Maven distro should be installed:

```
ant -Dmaven.home="$HOME/apps/maven/apache-maven-3.3.x-SNAPSHOT"
```

Once the build completes, you should have a new Maven distro ready to roll in that directory!
