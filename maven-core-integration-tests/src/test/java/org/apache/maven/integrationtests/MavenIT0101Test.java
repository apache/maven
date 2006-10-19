package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0101Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that properties defined in an active profile in the user's
        settings are available for interpolation of systemPath in a dependency.
        [MNG-2052] */
public void testit0101() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0101 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0101", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--settings settings.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("compile");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

