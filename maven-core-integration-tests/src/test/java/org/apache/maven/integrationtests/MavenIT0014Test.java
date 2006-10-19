package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0014Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test POM configuration by settings the -source and -target for the
        compiler to 1.4 */
public void testit0014() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0014 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0014", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("test");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

