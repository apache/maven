package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0104Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Verify that plugin configurations are resolved correctly, particularly
        when they contain ${project.build.directory} in the string value of a 
        Map.Entry. */
public void testit0104() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0104 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0104", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("test");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

