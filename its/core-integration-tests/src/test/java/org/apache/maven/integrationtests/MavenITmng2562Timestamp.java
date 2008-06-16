package org.apache.maven.integrationtests;

import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.it.Verifier;

import java.io.File;

public class MavenITmng2562Timestamp extends AbstractMavenIntegrationTestCase {
	
	public MavenITmng2562Timestamp()
	{
		super( "(2.0.9,)");
	}

    public void testitMNG2562() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(),
                "/mng-2562-timestamp");
        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.executeGoal("verify");

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
