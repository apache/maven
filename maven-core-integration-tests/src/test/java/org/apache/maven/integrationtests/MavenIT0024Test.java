package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0024Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test usage of &lt;executions/&gt; inside a plugin rather than &lt;goals/&gt;
        that are directly inside th plugin. */
public void testit0024() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0024 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0024", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("generate-sources");
verifier.assertFilePresent("target/classes/org/apache/maven/it0001/Person.class");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

