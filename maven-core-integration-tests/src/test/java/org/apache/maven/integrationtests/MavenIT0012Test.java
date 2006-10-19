package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0012Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test simple POM interpolation */
public void testit0012() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0012 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0012", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("core-it:touch");
verifier.assertFilePresent("target/touch-3.8.1.txt");
verifier.assertFilePresent("child-project/target/child-touch-3.0.3.txt");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

