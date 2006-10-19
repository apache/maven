package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0095Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test URL calculation when modules are in sibling dirs of parent. (MNG-2006) */
public void testit0095() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0095 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0095", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("integration-test");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

