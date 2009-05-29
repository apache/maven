package org.apache.maven;

import java.io.File;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Exclusion;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;

public class ProjectDependenciesResolverTest
    extends AbstractCoreMavenComponentTestCase
{
    @Requirement
    private ProjectDependenciesResolver resolver;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        resolver = lookup( ProjectDependenciesResolver.class );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        resolver = null;
        super.tearDown();
    }

    protected String getProjectsDirectory()
    {
        return "src/test/projects/project-dependencies-resolver";
    }

    public void testExclusionsInDependencies()
        throws Exception
    {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId( "commons-lang" );
        exclusion.setArtifactId( "commons-lang" );        
        
        MavenProject project = new ProjectBuilder( "org.apache.maven", "project-test", "1.0" )
            .addDependency( "org.apache.maven.its", "maven-core-it-support", "1.3", Artifact.SCOPE_RUNTIME, exclusion  )
            .get();        
        
        Set<Artifact> artifactDependencies = resolver.resolve( project, Artifact.SCOPE_COMPILE, getLocalRepository(), getRemoteRepositories() );
        assertEquals( 0, artifactDependencies.size() );
        
        artifactDependencies = resolver.resolve( project, Artifact.SCOPE_RUNTIME, getLocalRepository(), getRemoteRepositories() );
        assertEquals( 1, artifactDependencies.size() );
        assertEquals( "maven-core-it-support" , artifactDependencies.iterator().next().getArtifactId() );
    }
    
    public void testSystemScopeDependencies()
        throws Exception
    {
        MavenProject project = new ProjectBuilder( "org.apache.maven", "project-test", "1.0" )
            .addDependency( "com.mycompany", "system-dependency", "1.0", Artifact.SCOPE_SYSTEM, new File( getBasedir(), "pom.xml" ).getAbsolutePath() )
            .get();

        Set<Artifact> artifactDependencies = resolver.resolve( project, Artifact.SCOPE_COMPILE, getLocalRepository(), getRemoteRepositories() );                
        assertEquals( 1, artifactDependencies.size() );        
    }  
}
