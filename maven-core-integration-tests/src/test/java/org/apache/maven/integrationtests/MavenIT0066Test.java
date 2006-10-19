package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0066Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that nonstandard POM files will be installed correctly. */
public void testit0066() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0066 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0066", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("-f other-pom.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("install");
verifier.assertFilePresent("");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

