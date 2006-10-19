package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0006Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Integration test for the verifier plugin. */
public void testit0006() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0006 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0006", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("integration-test");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

