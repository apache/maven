-------------------------------------------------------------------------------
Bootstrapping Maven
-------------------------------------------------------------------------------

To bootstrap Maven you must have a ~/maven.properties file with the following
entries:

maven.home = /path/to/your/maven/installation
maven.repo.local = /path/to/your/local/repository

Once you have your ~/maven.properties setup then:

java -jar mboot.jar 

Should do the trick to produce a working installation of Maven
in ${maven.home}.

NOTE: You must run these instructions from this directory!
