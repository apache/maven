package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0089Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that Checkstyle PackageNamesLoader.loadModuleFactory(..) method will complete as-is with
        the context classloader available to the plugin.
        */
public void testit0089() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0089 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0089", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
verifier.executeGoal("install");
verifier.assertFilePresent("project/target/output.txt");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

