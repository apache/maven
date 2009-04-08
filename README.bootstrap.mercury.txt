BOOTSTRAPPING BASICS
-----------------------

You'll need:

- Java 1.5
- Ant 1.6.5 or later

First, give Ant a location into which the completed Maven distro should be installed:

    export M2_HOME=$HOME/apps/maven/apache-maven-3.0-SNAPSHOT

Then, run Ant:

    ant -f build-mercury.xml

You can use additiona options on ant command line:

-Dmaven.repo.update.policy={never|always|daily}
-Dmaven.repo.system={mercury|legacy}
-Dmaven.home=$HOME/apps/maven/apache-maven-3.0-SNAPSHOT

if you'd like to debug the bootstrap from Eclipse, uncomment the debugging options in the build-mercury.xml around 
line 310, then use the following commands:

For the first time - run the following, it will update the local repo

ant -f build-mercury.xml -Dmaven.repo.update.policy=always -Dmaven.repo.system=mercury

then you can run

ant -f build-mercury.xml -Dmaven.repo.update.policy=never -Dmaven.repo.system=mercury

not to bother with repo updates

 