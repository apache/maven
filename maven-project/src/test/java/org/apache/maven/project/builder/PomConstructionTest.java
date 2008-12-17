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

    public void testNexusPoms()
        throws Exception
    {        
        Map<String,File> artifacts = new HashMap<String,File>();
                
        File nexusAggregator = new File( testDirectory, "nexus/pom.xml" );
        File nexusParent = new File( testDirectory, "nexus/nexus-test-harness-parent/pom.xml" );
        File nexusLauncher = new File( testDirectory, "nexus/nexus-test-harness-launcher/pom.xml" );

        artifacts.put( "nexus-test-harness-parent", nexusParent );
        artifacts.put( "nexus-test-harness-launcher" , nexusLauncher );
        artifacts.put( "nexus-test-harness", nexusAggregator );
        
        PomArtifactResolver resolver = new FileBasedPomArtifactResolver( new File( BASE_POM_DIR, "nexus" ) );
                
        // make a version that doesn't require a null mixin set. for most pom construction tests we're
        // not going to use mixins.
        PomClassicDomainModel model = projectBuilder.buildModel( nexusLauncher, null, resolver );  
        
        assertEquals( 3, model.getLineageCount() );
        
        // This will get extremely tedious unless we can shorten these into small expressions to
        // retrieve the target values for testing.
        
        // model.build.plugins[0].executions
        // model/build/plugins[0].executions
        
        Model m = model.getModel();
        
        Plugin plugin = (Plugin) m.getBuild().getPlugins().get( 0 );
        
        List executions = plugin.getExecutions();
        
        //assertEquals( 7, executions.size() );
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
                    String id = fileName.substring( 0, i - 1 );
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
            artifact.setFile( artifacts.get(  artifact.getArtifactId() ) );
        }
    }
}
