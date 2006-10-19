package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0050Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test surefire inclusion/exclusions */
public void testit0050() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0050 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0050", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("target/testTouchFile.txt");
verifier.assertFilePresent("target/defaultTestTouchFile.txt");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

