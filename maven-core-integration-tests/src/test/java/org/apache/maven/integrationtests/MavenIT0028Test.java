package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0028Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that unused configuration parameters from the POM don't cause the
        mojo to fail...they will show up as warnings in the -X output instead. */
public void testit0028() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0028 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0028", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

