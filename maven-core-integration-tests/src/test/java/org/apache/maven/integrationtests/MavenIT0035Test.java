package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0035Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test artifact relocation. */
public void testit0035() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0035 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0035", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.1", "jar");
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.1", "pom");
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support-old-location", "1.1", "pom");
verifier.executeGoal("package");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support", "1.1", "jar");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support", "1.1", "pom");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support-old-location", "1.1", "pom");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

