package org.apache.maven.project.builder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.MavenTools;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.harness.PomTestWrapper;
import org.codehaus.plexus.PlexusTestCase;

public class PomConstructionTest
    extends PlexusTestCase
{
    private static String BASE_POM_DIR = "src/test/resources-project-builder";

    private ProjectBuilder projectBuilder;

    private MavenTools mavenTools;

    private File testDirectory;
    
    protected void setUp()
        throws Exception
    {
        testDirectory = new File( getBasedir(), BASE_POM_DIR );        
        projectBuilder = lookup( ProjectBuilder.class );
        mavenTools = lookup( MavenTools.class );
    }

    // Some better conventions for the test poms needs to be created and each of these tests
    // that represent a verification of a specification item needs to be a couple lines at most.
    // The expressions help a lot, but we need a clean to pick up a directory of POMs, automatically load
    // them into a resolver, create the expression to extract the data to validate the Model, and the URI
    // to validate the properties. We also need a way to navigate from the Tex specification documents to
    // the test in question and vice versa. A little Eclipse plugin would do the trick.
    public void testThatAllPluginExecutionsWithIdsAreJoined()
        throws Exception
    {        
        File nexusLauncher = new File( testDirectory, "nexus/nexus-test-harness-launcher/pom.xml" );        
        PomArtifactResolver resolver = artifactResolver( "nexus" );                
        PomClassicDomainModel model = projectBuilder.buildModel( nexusLauncher, null, resolver );         
        assertEquals( 3, model.getLineageCount() );        
        PomTestWrapper pom = new PomTestWrapper( model );        
        assertModelEquals( pom, "maven-dependency-plugin", "build/plugins[4]/artifactId" );        
        List executions = (List) pom.getValue( "build/plugins[4]/executions" );                
        assertEquals( 7, executions.size() );
    }

    public void testThatExecutionsWithoutIdsAreMergedAndTheChildWins()
        throws Exception
    {
        File pom = new File( testDirectory, "micromailer/micromailer-1.0.3.pom" );
        PomArtifactResolver resolver = artifactResolver( "micromailer" );
        PomClassicDomainModel model = projectBuilder.buildModel( pom, null, resolver );
        // This should be 2
        //assertEquals( 2, model.getLineageCount() );
        PomTestWrapper tester = new PomTestWrapper( model );
        assertModelEquals( tester, "child-descriptor", "build/plugins[1]/executions[1]/goals[1]" );
    }
    
    private PomArtifactResolver artifactResolver( String basedir )
    {
        PomArtifactResolver resolver = new FileBasedPomArtifactResolver( new File( BASE_POM_DIR, basedir ) );                
        return resolver;
    }
    
    protected void assertModelEquals( PomTestWrapper pom, Object expected, String expression )
    {
        assertEquals( expected, pom.getValue( expression ) );        
    }
    
    // Need to get this to walk around a directory and automatically build up the artifact set. If we
    // follow some standard conventions this can be simple.
    class FileBasedPomArtifactResolver
        implements PomArtifactResolver
    {
        private Map<String,File> artifacts = new HashMap<String,File>();
        
        private File basedir;
                
        public FileBasedPomArtifactResolver( File basedir )
        {
            this.basedir = basedir;
                        
            for ( File file : basedir.listFiles() )
            {
                String fileName = file.getName();                
                if ( file.getName().endsWith( ".pom" ) )
                {
                    int i = fileName.indexOf( ".pom" );                    
                    String id = fileName.substring( 0, i );
                    artifacts.put( id, file );
                }
            }
        }

        public FileBasedPomArtifactResolver( Map<String, File> artifacts )
        {
            this.artifacts = artifacts;
        }

        public void resolve( Artifact artifact )
            throws IOException
        {
            String id = artifact.getArtifactId() + "-" + artifact.getVersion();
            artifact.setFile( artifacts.get( id  ) );
        }
    }
}
