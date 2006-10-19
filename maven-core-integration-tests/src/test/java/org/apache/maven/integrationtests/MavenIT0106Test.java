package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0106Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** When a project has modules and its parent is not preinstalled [MNG-2318] */
public void testit0106() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0106 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0106", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("clean");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

