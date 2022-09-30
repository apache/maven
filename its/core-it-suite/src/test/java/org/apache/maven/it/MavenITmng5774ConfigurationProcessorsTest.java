package org.apache.maven.it;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class MavenITmng5774ConfigurationProcessorsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5774ConfigurationProcessorsTest()
    {
        super( "(3.2.5,)" );
    }

    @Test
    public void testBehaviourWhereThereIsOneUserSuppliedConfigurationProcessor()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5774-configuration-processors" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier = newVerifier( new File( testDir, "build-with-one-processor-valid" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-configuration-processors" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.executeGoal( "process-resources" );
        verifier.verifyErrorFreeLog();
        // Making sure our configuration processor executes
        verifier.verifyTextInLog( "[INFO] ConfigurationProcessorOne.process()" );
        // We have a property value injected by our configuration processor. Make sure it's correct
        verifier.verifyFilePresent( "target/classes/result.properties" );
        Properties result = verifier.loadProperties( "target/classes/result.properties" );
        assertEquals( "yes", result.getProperty( "configurationProcessorContributedValue" ) );
        verifier.resetStreams();
    }

    @Test
    public void testBehaviourWhereThereAreTwoUserSuppliedConfigurationProcessor()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5774-configuration-processors" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier = newVerifier( new File( testDir, "build-with-two-processors-invalid" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-configuration-processors" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( new File( testDir, "settings.xml" ).getAbsolutePath() );
        try
        {
            verifier.executeGoal( "process-resources" );
            fail( "We expected this invocation to fail because of too many user supplied configuration processors being present" );
        }
        catch ( VerificationException e )
        {
            verifier.verifyTextInLog( "There can only be one user supplied ConfigurationProcessor" );
        }
        verifier.resetStreams();
    }
}
