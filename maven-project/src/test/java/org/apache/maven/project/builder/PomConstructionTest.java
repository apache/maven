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
        
        PomArtifactResolver resolver = new FileBasedPomArtifactResolver( artifacts );
                
        PomClassicDomainModel model = projectBuilder.buildModel( nexusLauncher, null, resolver );  
        
        Model m = model.getModel();
        
        Plugin plugin = (Plugin) m.getBuild().getPlugins().get( 0 );
        
        List executions = plugin.getExecutions();
        
        //assertEquals( 7, executions.size() );
    }

    class FileBasedPomArtifactResolver
        implements PomArtifactResolver
    {
        private Map<String,File> artifacts = new HashMap<String,File>();
        
        public FileBasedPomArtifactResolver( Map<String, File> artifacts )
        {
            this.artifacts = artifacts;
        }

        public void resolve( Artifact artifact )
            throws IOException
        {
            System.out.println( artifact );
            artifact.setFile( artifacts.get(  artifact.getArtifactId() ) );
        }
    }
}
