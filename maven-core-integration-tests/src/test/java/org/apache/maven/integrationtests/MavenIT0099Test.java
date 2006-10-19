package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0099Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that parent-POMs cached during a build are available as parents
        to other POMs in the multimodule build. [MNG-2130] */
public void testit0099() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0099 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0099", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.it0099", "maven-it0099-parent", "1", "pom");
verifier.executeGoal("package");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

