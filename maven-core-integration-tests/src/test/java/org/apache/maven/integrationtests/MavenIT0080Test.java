package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0080Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that depending on a WAR doesn't also get its dependencies
        transitively. */
public void testit0080() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0080 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0080", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("test-component-a/target/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-b/target/test-component-b-0.1.war");
verifier.assertFilePresent("test-component-b/target/test-component-b-0.1.war!/WEB-INF/lib/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.ear");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.ear!/test-component-b-0.1.war");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1/test-component-b-0.1.war");
verifier.assertFileNotPresent("test-component-c/target/test-component-c-0.1/test-component-a-0.1.jar");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

