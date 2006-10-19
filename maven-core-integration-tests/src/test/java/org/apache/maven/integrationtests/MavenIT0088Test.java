package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0088Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test path translation. */
public void testit0088() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0088 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0088", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("target/classes/test.properties");
verifier.assertFilePresent("target/mojo-generated.properties");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

