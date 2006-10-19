package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0060Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test aggregation of list configuration items when using
        'combine.children=append' attribute. Specifically, merge the list of
        excludes for the testCompile mojo. */
public void testit0060() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0060 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0060", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("subproject/target/classes/org/apache/maven/it0001/Person.class");
verifier.assertFilePresent("subproject/target/test-classes/org/apache/maven/it0001/PersonTest.class");
verifier.assertFileNotPresent("subproject/target/test-classes/org/apache/maven/it0001/PersonTwoTest.class");
verifier.assertFileNotPresent("subproject/target/test-classes/org/apache/maven/it0001/PersonThreeTest.class");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

