package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0052Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that source attachment doesn't take place when
        -DperformRelease=true is missing. */
public void testit0052() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0052 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0052", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--no-plugin-registry");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0051-1.0.jar");
verifier.assertFileNotPresent("target/maven-core-it0051-1.0-sources.jar");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

