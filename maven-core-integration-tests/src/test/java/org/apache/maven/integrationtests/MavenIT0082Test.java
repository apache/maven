package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0082Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that the reactor can establish the artifact location of known projects for dependencies
        using process-sources to see that it works even when they aren't compiled */
public void testit0082() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0082 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0082", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("process-sources");
verifier.assertFilePresent("test-component-c/target/my-test");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

