package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0068Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test repository accumulation. */
public void testit0068() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0068 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0068", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.deleteArtifact("org.codehaus.modello", "modello-core", "1.0-alpha-3", "jar");
Properties verifierProperties = new Properties();
verifierProperties.put("failOnErrorOutput", "false");
verifier.setVerifierProperties(verifierProperties);
verifier.executeGoal("generate-sources");
verifier.assertFilePresent("target/generated-sources/modello/org/apache/maven/settings/Settings.java");
// don't verify error free log
verifier.resetStreams();
System.out.println("PASS");
}}

