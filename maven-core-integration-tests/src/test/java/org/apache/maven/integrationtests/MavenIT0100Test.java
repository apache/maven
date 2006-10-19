package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0100Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that ${parent.artifactId} resolves correctly. [MNG-2124] */
public void testit0100() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0100 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0100", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("verify");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

