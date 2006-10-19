package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0053Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that attached artifacts have the same buildnumber and timestamp
        as the main artifact. This will not correctly verify until we have
        some way to pattern-match the buildnumber/timestamp... */
public void testit0053() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0053 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0053", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--no-plugin-registry");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0053-1.0-SNAPSHOT.jar");
verifier.assertFileNotPresent("target/maven-core-it0053-1.0-SNAPSHOT-sources.jar");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

