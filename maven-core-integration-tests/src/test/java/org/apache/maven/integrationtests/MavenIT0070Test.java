package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0070Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test a RAR generation. */
public void testit0070() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0070 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0070", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0070-1.0.rar");
verifier.assertFilePresent("target/maven-core-it0070-1.0.rar!/META-INF/ra.xml");
verifier.assertFilePresent("target/maven-core-it0070-1.0.rar!/SomeResource.txt");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

