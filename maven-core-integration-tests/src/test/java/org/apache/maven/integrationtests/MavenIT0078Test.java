package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0078Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that configuration for maven-compiler-plugin is injected from
        PluginManagement section even when it's not explicitly defined in the
        plugins section. */
public void testit0078() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0078 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0078", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("compile");
verifier.assertFileNotPresent("target/classes/Test.class");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

