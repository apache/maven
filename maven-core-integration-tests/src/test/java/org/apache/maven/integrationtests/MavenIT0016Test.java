package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0016Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test a WAR generation */
public void testit0016() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0016 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0016", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0016/Person.class");
verifier.assertFilePresent("target/maven-core-it0016-1.0/index.html");
verifier.assertFilePresent("target/maven-core-it0016-1.0/WEB-INF/classes/org/apache/maven/it0016/Person.class");
verifier.assertFilePresent("target/maven-core-it0016-1.0/WEB-INF/lib/commons-logging-1.0.3.jar");
verifier.assertFileNotPresent("target/maven-core-it0016-1.0/WEB-INF/lib/servletapi-2.4-20040521.jar");
verifier.assertFilePresent("target/maven-core-it0016-1.0.war");
verifier.assertFilePresent("target/maven-core-it0016-1.0.war!/index.html");
verifier.assertFilePresent("target/maven-core-it0016-1.0.war!/WEB-INF/classes/org/apache/maven/it0016/Person.class");
verifier.assertFilePresent("target/maven-core-it0016-1.0.war!/WEB-INF/lib/commons-logging-1.0.3.jar");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

