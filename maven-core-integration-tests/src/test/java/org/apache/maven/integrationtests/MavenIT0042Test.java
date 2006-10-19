package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0042Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that the reactor can establish the artifact location of known projects for dependencies */
public void testit0042() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0042 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0042", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("test-component-a/target/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-b/target/test-component-b-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/test-component-b-0.1.jar");
verifier.assertFilePresent("test-component-c/target/my-test");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

