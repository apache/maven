package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0051Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test source attachment when -DperformRelease=true is specified. */
public void testit0051() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0051 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0051", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--no-plugin-registry -DperformRelease=true");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0051-1.0.jar");
verifier.assertFilePresent("target/maven-core-it0051-1.0-sources.jar");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

