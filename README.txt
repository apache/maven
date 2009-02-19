-------------------------------------------------------------------------------
Bootstrapping Maven
-------------------------------------------------------------------------------

Set the environment variable M2_HOME pointing to the dir where you want Maven2 installed.

NOTE: presently, the directory {M2_HOME}/bin must be in your path:
set PATH=%PATH%;%M2_HOME%\bin
or
export PATH=$PATH:$M2_HOME/bin

In addition, the last part of the M2_HOME path MUST be of the form maven-$version, eg:
/usr/local/apache-maven-2.1.0-SNAPSHOT

You can set the parameters passed to the Java VM when running Maven2 bootstrap,
setting the environment variable MAVEN_OPTS, e.g.
e.g. to run in offline mode, set MAVEN_OPTS=-o

Then run `ant`.

NOTE: You must run these instructions from this directory!

If you are behind a firewall, you will need to let the bootstrap process know.
To do this, create a file at ~/.m2/settings.xml and paste in the XML below,
substituting your settings for those provided. You can safely skip the
username, password and nonProxyHost elements if they are not relevant to you.

<settings>
  <proxies>
    <proxy>
      <active>true</active>
      <protocol>http</protocol>
      <host>proxy.somewhere.com</host>
      <port>8080</port>
      <username>proxyuser</username>
      <password>somepassword</password>
      <nonProxyHosts>www.google.com|*.somewhere.com</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
