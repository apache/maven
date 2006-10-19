package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0084Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Verify that the collector selecting a particular version gets the correct subtree */
public void testit0084() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0084 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0084", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("package");
verifier.assertFilePresent("test-component-a/target/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-b/target/test-component-b-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/test-component-b-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/maven-core-it-support-1.4.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1.war!/WEB-INF/lib/commons-io-1.0.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/test-component-a-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/test-component-b-0.1.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/maven-core-it-support-1.4.jar");
verifier.assertFilePresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/commons-io-1.0.jar");
verifier.assertFileNotPresent("test-component-c/target/test-component-c-0.1/WEB-INF/lib/commons-lang-1.0.jar");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

