package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0056Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that multiple executions of the compile goal with different
        includes/excludes will succeed. */
public void testit0056() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0056 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0056", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("test-compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/PersonTwo.class");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/PersonThree.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0001/PersonTest.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0001/PersonTwoTest.class");
verifier.assertFilePresent("target/test-classes/org/apache/maven/it0001/PersonThreeTest.class");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

