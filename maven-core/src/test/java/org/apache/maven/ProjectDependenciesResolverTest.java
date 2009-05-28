package org.apache.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
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

    public void testCalculationOfBuildPlanWithIndividualTaskOfTheCleanCleanGoal()
        throws Exception
    {
        MavenProject project = createProject();
        
        Set<Artifact> artifactDependencies = resolver.resolve( project, Artifact.SCOPE_COMPILE, getLocalRepository(), getRemoteRepositories() );
        assertEquals( 0, artifactDependencies.size() );
        
        artifactDependencies = resolver.resolve( project, Artifact.SCOPE_RUNTIME, getLocalRepository(), getRemoteRepositories() );
        assertEquals( 1, artifactDependencies.size() );
        assertEquals( "maven-core-it-support" , artifactDependencies.iterator().next().getArtifactId() );
    }
    
    private MavenProject createProject()
    {
        Model model = new Model();
        model.setModelVersion( "4.0.0" );
        model.setGroupId( "org.apache.maven" );
        model.setArtifactId( "project-test" );
        model.setVersion( "1.0" );  
        
        List<Dependency> dependencies = new ArrayList<Dependency>();
        dependencies.add( d( "org.apache.maven.its", "maven-core-it-support", "1.3" ) );
        model.setDependencies( dependencies );        
        
        return new MavenProject( model );
    }
    
    private Dependency d( String g, String a, String v )
    {
        Dependency d = new Dependency();
        d.setGroupId( g );
        d.setArtifactId( a );
        d.setVersion( v );
        d.setScope( Artifact.SCOPE_RUNTIME );
        
        Exclusion e = new Exclusion();
        e.setGroupId( "commons-lang" );
        e.setArtifactId( "commons-lang" );        
        d.addExclusion( e );
        
        return d;
    }    
}
