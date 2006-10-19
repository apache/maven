package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0061Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Verify that deployment of artifacts to a legacy-layout repository
        results in a groupId directory of 'the.full.group.id' instead of
        'the/full/group/id'. */
public void testit0061() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0061 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0061", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("deploy");
verifier.assertFilePresent("target/test-repo/org.apache.maven.it/jars/maven-core-it0061-1.0.jar");
verifier.assertFilePresent("target/test-repo/org.apache.maven.it/poms/maven-core-it0061-1.0.pom");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

