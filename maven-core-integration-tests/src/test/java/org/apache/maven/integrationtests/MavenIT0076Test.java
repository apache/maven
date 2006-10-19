package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0076Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that plugins in pluginManagement aren't included in the build
        unless they are referenced by groupId/artifactId within the plugins
        section of a pom. */
public void testit0076() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0076 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0076", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("install");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

