package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0037Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test building with alternate pom file using '-f' */
public void testit0037() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0037 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0037", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("-f pom2.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("package");
verifier.assertFilePresent("target/maven-core-it0037-1.0-build2.jar");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

