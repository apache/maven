package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0005Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** The simplest of pom installation. We have a snapshot pom and we install
        it in local repository. */
public void testit0005() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0005 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0005", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven", "maven-core-it0005", "1.0-SNAPSHOT", "pom");
verifier.executeGoal("install:install");
verifier.assertArtifactPresent("org.apache.maven", "maven-core-it0005", "1.0-SNAPSHOT", "pom");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

