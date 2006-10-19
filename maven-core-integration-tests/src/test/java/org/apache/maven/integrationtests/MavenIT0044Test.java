package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0044Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test --settings CLI option */
public void testit0044() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0044 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0044", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
List cliOptions = new ArrayList();
cliOptions.add("--settings settings.xml");
verifier.setCliOptions(cliOptions);
verifier.executeGoal("core-it:touch");
verifier.assertFilePresent("target/test.txt");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

