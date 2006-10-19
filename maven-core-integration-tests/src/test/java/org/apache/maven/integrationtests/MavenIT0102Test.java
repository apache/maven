package org.apache.maven.integrationtests;
import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.maven.it.*;
import org.apache.maven.it.util.*;

public class MavenIT0102Test extends TestCase /*extends AbstractMavenIntegrationTest*/ {    

/** Test that &lt;activeByDefault/&gt; calculations for profile activation only
        use profiles defined in the POM. [MNG-2136] */
public void testit0102() throws Exception {
String basedir = System.getProperty("maven.test.tmpdir", System.getProperty("java.io.tmpdir"));
File testDir = new File(basedir, getName());
FileUtils.deleteDirectory(testDir);
System.out.println("Extracting it0102 to " + testDir.getAbsolutePath());
ResourceExtractor.extractResourcePath(getClass(), "/it0102", testDir);
Verifier verifier = new Verifier(testDir.getAbsolutePath());
verifier.executeGoal("verify");
verifier.verifyErrorFreeLog();
verifier.resetStreams();
System.out.println("PASS");
}}

