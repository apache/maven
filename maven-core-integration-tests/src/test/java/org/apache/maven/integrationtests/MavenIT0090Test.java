package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0090Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that ensures that envars are interpolated correctly into plugin
        configurations. */
public void testit0090() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0090 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0090", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("target/mojo-generated.properties");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

