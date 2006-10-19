package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0048Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Verify that default values for mojo parameters are working (indirectly, 
        by verifying that the Surefire mojo is functioning correctly). */
public void testit0048() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0048 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0048", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("target/testFileOutput.txt");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

