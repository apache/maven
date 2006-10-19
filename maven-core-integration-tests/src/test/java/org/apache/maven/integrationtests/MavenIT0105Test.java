package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0105Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** MRESOURCES-18 */
public void testit0105() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0105 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0105", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("-Dparam=PARAM");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("test");
verifier.assertFilePresent("target/classes/test.properties");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

