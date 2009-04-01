package org.apache.maven;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MavenPluginCollector;
import org.apache.maven.plugin.MavenPluginDiscoverer;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.FileUtils;

public abstract class AbstractCoreMavenComponentTestCase
    extends PlexusTestCase
{
    @Requirement
    protected RepositorySystem repositorySystem;

    @Requirement
    protected MavenProjectBuilder projectBuilder;
    
    protected void setUp()
        throws Exception
    {
        super.setUp();
        repositorySystem = lookup( RepositorySystem.class );
        projectBuilder = lookup( MavenProjectBuilder.class );                
    }

    abstract protected String getProjectsDirectory();
        
    protected File getProject( String name )
        throws Exception
    {
        File source = new File( new File( getBasedir(), getProjectsDirectory() ), name );
        File target = new File( new File ( getBasedir(), "target" ), name );
        if ( !target.exists() )
        {
            FileUtils.copyDirectoryStructure( source, target );
        }
        return new File( target, "pom.xml" );
    }   
    
    /**
     * We need to customize the standard Plexus container with the plugin discovery listener which
     * is what looks for the META-INF/maven/plugin.xml resources that enter the system when a
     * Maven plugin is loaded.
     * 
     * We also need to customize the Plexus container with a standard plugin discovery listener
     * which is the MavenPluginCollector. When a Maven plugin is discovered the MavenPluginCollector
     * collects the plugin descriptors which are found. 
     */
    protected void customizeContainerConfiguration( ContainerConfiguration containerConfiguration )
    {
        containerConfiguration.addComponentDiscoverer( new MavenPluginDiscoverer() );
        containerConfiguration.addComponentDiscoveryListener( new MavenPluginCollector() );
    }
    
    protected MavenExecutionRequest createMavenExecutionRequest( File pom )
        throws Exception
    {
        ArtifactRepository localRepository = repositorySystem.createDefaultLocalRepository();
        ArtifactRepository remoteRepository = repositorySystem.createDefaultRemoteRepository();
        
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setPom( pom )
            .setProjectPresent( true )
            .setPluginGroups( Arrays.asList( new String[] { "org.apache.maven.plugins" } ) )
            .setLocalRepository( localRepository )
            .setRemoteRepositories( Arrays.asList( remoteRepository ) )
            .setGoals( Arrays.asList( new String[] { "package" } ) )   
            .setProperties( new Properties() );        
        
        return request;
    }
    
    // layer the creation of a project builder configuration with a request, but this will need to be
    // a Maven subclass because we don't want to couple maven to the project builder which we need to
    // separate.
    protected MavenSession createMavenSession( File pom )
        throws Exception
    {
        MavenExecutionRequest request = createMavenExecutionRequest( pom );

        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration()
            .setLocalRepository( request.getLocalRepository() )
            .setRemoteRepositories( request.getRemoteRepositories() );

        MavenProject project = null;
        
        if ( pom != null )
        {
            project = projectBuilder.build( pom, configuration );
        }
        else
        {
            project = createStubMavenProject();
        }
                        
        MavenSession session = new MavenSession( getContainer(), request, project );
        
        return session;
    }      
    
    protected MavenProject createStubMavenProject()
    {
        Model model = new Model();
        model.setGroupId( "org.apache.maven.test" );
        model.setArtifactId( "maven-test" );
        model.setVersion( "1.0" );
        return new MavenProject( model );        
    }
}
