package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0047Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test the use case for having a compile time dependency be transitive: 
        when you extend a class you need its dependencies at compile time. */
public void testit0047() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0047 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0047", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("compile");
verifier.assertFilePresent("target/classes/org/apache/maven/it0047/Person.class");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

