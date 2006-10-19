package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0017Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test an EJB generation */
public void testit0017() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0017 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0017", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0017/Person.class");
verifier.assertFilePresent("target/maven-core-it0017-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0017-1.0.jar!/META-INF/ejb-jar.xml");
verifier.assertFilePresent("target/maven-core-it0017-1.0.jar!/org/apache/maven/it0017/Person.class");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

