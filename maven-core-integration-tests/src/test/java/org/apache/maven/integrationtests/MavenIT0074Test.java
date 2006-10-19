package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0074Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that plugin-level configuration instances are not nullified by
        execution-level configuration instances. */
public void testit0074() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0074 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0074", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("eclipse:eclipse");
verifier.assertFilePresent(".classpath");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

