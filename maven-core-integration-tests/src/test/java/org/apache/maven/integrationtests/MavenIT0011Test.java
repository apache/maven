package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0011Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test specification of dependency versions via &lt;dependencyManagement/&gt;. */
public void testit0011() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0011 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0011", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0011/PersonFinder.class");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

