package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0019Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that a version is managed by pluginManagement in the super POM */
public void testit0019() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0019 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0019", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0019/Person.class");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

