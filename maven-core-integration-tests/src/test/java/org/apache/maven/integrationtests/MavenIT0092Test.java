package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0092Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that legacy repositories with legacy snapshots download correctly. */
public void testit0092() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0092 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0092", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar");
verifier.executeGoal("compile");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it-support", "1.0-SNAPSHOT", "jar");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

