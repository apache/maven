package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0077Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test test jar attachment. */
public void testit0077() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0077 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0077", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.it", "maven-it0077-sub1", "1.0", "test-jar");
List cliOptions = new ArrayList();
cliOptions.add("--settings settings.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("install");
verifier.assertArtifactPresent("org.apache.maven.it", "maven-it0077-sub1", "1.0", "test-jar");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

