package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0046Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test fail-never reactor behavior. Forces an exception to be thrown in
        the first module, but checks that the second modules is built. */
public void testit0046() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0046 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0046", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.deleteArtifact("org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin");
List cliOptions = new ArrayList();
cliOptions.add("--no-plugin-registry --fail-never");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("core-it:touch");
verifier.assertFilePresent("target/touch.txt");
verifier.assertFileNotPresent("subproject/target/touch.txt");
verifier.assertFilePresent("subproject2/target/touch.txt");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

