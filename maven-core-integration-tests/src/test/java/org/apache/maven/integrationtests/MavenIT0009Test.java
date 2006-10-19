package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0009Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test plugin configuration and goal configuration that overrides what the
        mojo has specified.
         */
public void testit0009() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0009 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0009", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
verifier.executeGoal("generate-resources");
verifier.assertFilePresent("target/pluginItem");
verifier.assertFilePresent("target/goalItem");
verifier.assertFileNotPresent("target/bad-item");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

