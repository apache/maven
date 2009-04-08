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

if you'd like to debug the bootstrap from Eclipse, issue the following command:

export ANT_OPTS='-Dmercury.log.level=info -Dmercury.dump.tree=../forest'
export ANT_OPTS=$ANT_OPTS' -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000'

the additional options:
-Dmercury.log.level={info|warn|error|debug} - defines mercury verbocity
-Dmercury.dump.tree=../forest - tells mercury to spit all the resolved trees into files ../forest-xxx.xml, where xxx is a timestamp

Then connect Eclipse debugging session to local port 8000

for example:

For the first time - run the following, it will update the local repo

export ANT_OPTS='-Dmercury.log.level=info -Dmercury.dump.tree=../forest'
export ANT_OPTS=$ANT_OPTS' -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000'
ant -f build-mercury.xml -Dmaven.repo.update.policy=always

then you can run

ant -f build-mercury.xml -Dmaven.repo.update.policy=never -Dmaven.repo.system=mercury

to debug the bootstrap

 