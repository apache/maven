package org.apache.maven;

import java.io.File;
import java.nio.file.Files;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class DefaultMavenTest
    extends AbstractCoreMavenComponentTestCase
{

    @Inject
    private Maven maven;

    @Override
    protected String getProjectsDirectory()
    {
        return "src/test/projects/default-maven";
    }


    @Test
    public void testThatErrorDuringProjectDependencyGraphCreationAreStored()
            throws Exception
    {
        MavenExecutionRequest request = createMavenExecutionRequest( getProject( "cyclic-reference" ) ).setGoals( asList("validate") );

        MavenExecutionResult result = maven.execute( request );

        assertEquals( ProjectCycleException.class, result.getExceptions().get( 0 ).getClass() );
    }

    @Test
    public void testMavenProjectNoDuplicateArtifacts()
        throws Exception
    {
        MavenProjectHelper mavenProjectHelper = lookup( MavenProjectHelper.class );
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "g", "a", "1.0", Artifact.SCOPE_TEST, "jar", "", null ) );
        File artifactFile = Files.createTempFile( "foo", "tmp").toFile();
        try
        {
            mavenProjectHelper.attachArtifact( mavenProject, "sources", artifactFile );
            assertEquals( 1, mavenProject.getAttachedArtifacts().size() );
            mavenProjectHelper.attachArtifact( mavenProject, "sources", artifactFile );
            assertEquals( 1, mavenProject.getAttachedArtifacts().size() );
        } finally
        {
            Files.deleteIfExists( artifactFile.toPath() );
        }
    }

}
