package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0094Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test classloading issues with mojos after 2.0 (MNG-1898). */
public void testit0094() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0094 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0094", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("install");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

