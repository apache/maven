package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0059Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Verify that maven-1 POMs will be ignored but not stop the resolution
        process. */
public void testit0059() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0059 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0059", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
Properties verifierProperties = new Properties();
verifierProperties.put("failOnErrorOutput", "false");
verifier.setVerifierProperties(verifierProperties);
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0059-1.0.jar");
// don't verify error free log
verifier.resetStreams();
System.out.println("PASS");
}}

