package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0020Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test beanshell mojo support. */
public void testit0020() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0020 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0020", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-it0020-plugin", "1.0-SNAPSHOT", "maven-plugin");
FileUtils.deleteFile(new File(basedir, "target/out.txt"));
List goals = Arrays.asList(new String[] {"install", "it0020:it0020"});
verifier.executeGoals(goals);
verifier.assertFilePresent("target/out.txt");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

