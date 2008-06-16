package org.apache.maven.integrationtests;

import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.it.Verifier;

import java.io.File;

public class MavenITmng3535SelfReferentialProperties extends AbstractMavenIntegrationTestCase {
	
	public MavenITmng3535SelfReferentialProperties()
	{
		super( "(2.0.9,)");
	}

    public void testitMNG3535() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(),
                "/mng-3535-selfReferentialProperties");
        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.executeGoal("verify");

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
