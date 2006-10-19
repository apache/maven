package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0054Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test resource filtering. */
public void testit0054() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0054 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0054", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/classes/org/apache/maven/it0054/Person.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0054/PersonTest.class");
verifier.assertFilePresent("target/maven-core-it0054-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0054-1.0.jar!/it0054.properties");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

