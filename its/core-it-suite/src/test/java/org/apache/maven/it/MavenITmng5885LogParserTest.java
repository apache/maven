package src.test.java.org.apache.maven.it;

import org.apache.maven.it.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class MavenITmng5885LogParserTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testGood() throws Exception {
        Path testDir = extractResources("mng-5885");

        // First, build the test extension
        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("package");
        verifier.addCliArgument("compile");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        //Your Maven Goals contain duplicate lifecycle phases that lead to multiple executions of the same phase: [compile, after:sources, resources, after:resources, initialize, validate]. If this is not intended, you should check your Maven Build.
        verifier.verifyTextInLog("Your Maven Goals contain duplicate lifecycle");
    }
}
