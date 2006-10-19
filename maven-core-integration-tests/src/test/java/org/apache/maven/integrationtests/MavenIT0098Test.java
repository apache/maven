package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0098Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that quoted system properties are processed correctly. [MNG-1415] */
public void testit0098() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0098 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0098", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
FileUtils.deleteDirectory(new File(basedir, "${basedir}/test project"));
List cliOptions = new ArrayList();
cliOptions.add("-Dtest.property=\"Test Property\"");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("test");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

