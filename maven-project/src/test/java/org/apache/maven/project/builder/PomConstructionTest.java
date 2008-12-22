package org.apache.maven.project.builder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.MavenTools;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.harness.PomTestWrapper;
import org.codehaus.plexus.PlexusTestCase;

public class PomConstructionTest
    extends PlexusTestCase
{

    private static String BASE_POM_DIR = "src/test/resources-project-builder";

    private ProjectBuilder projectBuilder;

    private MavenTools mavenTools;

    private PomArtifactResolver pomArtifactResolver;

    private File testDirectory;

    protected void setUp()
        throws Exception
    {
        testDirectory = new File( getBasedir(), BASE_POM_DIR );
        projectBuilder = lookup( ProjectBuilder.class );
        mavenTools = lookup( MavenTools.class );
        pomArtifactResolver = new PomArtifactResolver()
        {

            public void resolve( Artifact artifact )
                throws IOException
            {
                throw new IllegalStateException( "Parent POM should be locally reachable " + artifact );
            }

        };
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
        List<?> executions = (List<?>) pom.getValue( "build/plugins[4]/executions" );                
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

    public void testErroneousJoiningOfDifferentPluginsWithEqualDependencies()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "equal-plugin-deps" );
        assertEquals( "maven-it-plugin-a", pom.getValue( "build/plugins[1]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/dependencies" ) ).size() );
        assertEquals( "maven-it-plugin-b", pom.getValue( "build/plugins[2]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/dependencies" ) ).size() );
    }

    /* FIXME: cf. MNG-3821
    public void testErroneousJoiningOfDifferentPluginsWithEqualExecutionIds()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "equal-plugin-exec-ids" );
        assertEquals( "maven-it-plugin-a", pom.getValue( "build/plugins[1]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "maven-it-plugin-b", pom.getValue( "build/plugins[2]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "maven-it-plugin-a", pom.getValue( "reporting/plugins[1]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "reporting/plugins[1]/reportSets" ) ).size() );
        assertEquals( "maven-it-plugin-b", pom.getValue( "reporting/plugins[2]/artifactId" ) );
        assertEquals( 1, ( (List<?>) pom.getValue( "reporting/plugins[1]/reportSets" ) ).size() );
    }
    */

    /* FIXME: cf. MNG-3886
    public void testOrderOfGoalsFromPluginExecutionWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-goals-order/wo-plugin-mngt" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/goals" ) ).size() );
        assertEquals( "b", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "a", pom.getValue( "build/plugins[1]/executions[1]/goals[2]" ) );
        assertEquals( "d", pom.getValue( "build/plugins[1]/executions[1]/goals[3]" ) );
        assertEquals( "c", pom.getValue( "build/plugins[1]/executions[1]/goals[4]" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/executions[1]/goals[5]" ) );
    }

    public void testOrderOfGoalsFromPluginExecutionWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-goals-order/w-plugin-mngt" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/goals" ) ).size() );
        assertEquals( "b", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "a", pom.getValue( "build/plugins[1]/executions[1]/goals[2]" ) );
        assertEquals( "d", pom.getValue( "build/plugins[1]/executions[1]/goals[3]" ) );
        assertEquals( "c", pom.getValue( "build/plugins[1]/executions[1]/goals[4]" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/executions[1]/goals[5]" ) );
    }
    //*/

    /* FIXME: cf. MNG-3887
    public void testOrderOfPluginExecutionsWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-order/wo-plugin-mngt" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "b", pom.getValue( "build/plugins[1]/executions[1]/id" ) );
        assertEquals( "a", pom.getValue( "build/plugins[1]/executions[2]/id" ) );
        assertEquals( "d", pom.getValue( "build/plugins[1]/executions[3]/id" ) );
        assertEquals( "c", pom.getValue( "build/plugins[1]/executions[4]/id" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/executions[5]/id" ) );
    }

    public void testOrderOfPluginExecutionsWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "plugin-exec-order/w-plugin-mngt" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "b", pom.getValue( "build/plugins[1]/executions[1]/id" ) );
        assertEquals( "a", pom.getValue( "build/plugins[1]/executions[2]/id" ) );
        assertEquals( "d", pom.getValue( "build/plugins[1]/executions[3]/id" ) );
        assertEquals( "c", pom.getValue( "build/plugins[1]/executions[4]/id" ) );
        assertEquals( "e", pom.getValue( "build/plugins[1]/executions[5]/id" ) );
    }
    //*/

    /* FIXME: cf. MNG-3925
    public void testOrderOfMergedPluginExecutionsWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "merged-plugin-exec-order/wo-plugin-mngt/sub" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "parent-1", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "parent-2", pom.getValue( "build/plugins[1]/executions[2]/goals[1]" ) );
        assertEquals( "child-default", pom.getValue( "build/plugins[1]/executions[3]/goals[1]" ) );
        assertEquals( "child-1", pom.getValue( "build/plugins[1]/executions[4]/goals[1]" ) );
        assertEquals( "child-2", pom.getValue( "build/plugins[1]/executions[5]/goals[1]" ) );
    }

    public void testOrderOfMergedPluginExecutionsWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "merged-plugin-exec-order/w-plugin-mngt/sub" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions" ) ).size() );
        assertEquals( "parent-1", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "parent-2", pom.getValue( "build/plugins[1]/executions[2]/goals[1]" ) );
        assertEquals( "child-default", pom.getValue( "build/plugins[1]/executions[3]/goals[1]" ) );
        assertEquals( "child-1", pom.getValue( "build/plugins[1]/executions[4]/goals[1]" ) );
        assertEquals( "child-2", pom.getValue( "build/plugins[1]/executions[5]/goals[1]" ) );
    }
    //*/

    /* FIXME: cf. MNG-3937
    public void testOrderOfMergedPluginExecutionGoalsWithoutPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "merged-plugin-exec-goals-order/wo-plugin-mngt/sub" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/goals" ) ).size() );
        assertEquals( "child-a", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "merged", pom.getValue( "build/plugins[1]/executions[1]/goals[2]" ) );
        assertEquals( "child-b", pom.getValue( "build/plugins[1]/executions[1]/goals[3]" ) );
        assertEquals( "parent-b", pom.getValue( "build/plugins[1]/executions[1]/goals[4]" ) );
        assertEquals( "parent-a", pom.getValue( "build/plugins[1]/executions[1]/goals[5]" ) );
    }

    public void testOrderOfMergedPluginExecutionGoalsWithPluginManagement()
        throws Exception
    {
        PomTestWrapper pom = buildPom( "merged-plugin-exec-goals-order/w-plugin-mngt/sub" );
        assertEquals( 5, ( (List<?>) pom.getValue( "build/plugins[1]/executions[1]/goals" ) ).size() );
        assertEquals( "child-a", pom.getValue( "build/plugins[1]/executions[1]/goals[1]" ) );
        assertEquals( "merged", pom.getValue( "build/plugins[1]/executions[1]/goals[2]" ) );
        assertEquals( "child-b", pom.getValue( "build/plugins[1]/executions[1]/goals[3]" ) );
        assertEquals( "parent-b", pom.getValue( "build/plugins[1]/executions[1]/goals[4]" ) );
        assertEquals( "parent-a", pom.getValue( "build/plugins[1]/executions[1]/goals[5]" ) );
    }
    //*/

    private PomArtifactResolver artifactResolver( String basedir )
    {
        return new FileBasedPomArtifactResolver( new File( BASE_POM_DIR, basedir ) );
    }

    private PomTestWrapper buildPom( String pomPath )
        throws IOException
    {
        File pomFile = new File( testDirectory, pomPath );
        if ( pomFile.isDirectory() )
        {
            pomFile = new File( pomFile, "pom.xml" );
        }
        return new PomTestWrapper( projectBuilder.buildModel( pomFile, null, pomArtifactResolver ) );
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
