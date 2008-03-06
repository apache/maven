package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MavenITmng3341MetadataUpdatedFromDeploymentRepositoryTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3341MetadataUpdatedFromDeploymentRepositoryTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" );
    }

    public void testitMNG3341()
        throws Exception
    {
        File testDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/mng-3341-metadataUpdatedFromDeploymentRepository" );

        File targetRepository = new File( testDir, "target-repository" );
        FileUtils.deleteDirectory( targetRepository );
        FileUtils.copyDirectoryStructure( new File( testDir, "deployment-repository" ), targetRepository );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "-s" );
        cliOptions.add( "settings.xml" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "deploy" );

        verifier.verifyErrorFreeLog();

        Xpp3Dom dom = readDom( new File( targetRepository,
                                         "org/apache/maven/its/mng3341/test-artifact/1.0-SNAPSHOT/maven-metadata.xml" ) );
        assertEquals( "2", dom.getChild( "versioning" ).getChild( "snapshot" ).getChild( "buildNumber" ).getValue() );

        dom = readDom( new File( targetRepository, "org/apache/maven/its/mng3341/maven-metadata.xml" ) );
        Xpp3Dom[] plugins = dom.getChild( "plugins" ).getChildren();
        assertEquals( "other-plugin", plugins[0].getChild( "prefix" ).getValue() );
        assertEquals( "test-artifact", plugins[1].getChild( "prefix" ).getValue() );

        verifier.resetStreams();
    }

    private Xpp3Dom readDom( File file )
        throws XmlPullParserException, IOException
    {
        FileReader reader = new FileReader( file );
        try
        {
            return Xpp3DomBuilder.build( reader );
        }
        finally
        {
            reader.close();
        }
    }
}