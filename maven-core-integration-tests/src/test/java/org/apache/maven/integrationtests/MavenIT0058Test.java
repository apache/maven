package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0058Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Verify that profiles from settings.xml do not pollute module lists
        across projects in a reactorized build. */
public void testit0058() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0058 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0058", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--settings ./settings.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("subproject/target/maven-core-it0058-subproject-1.0.jar");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

