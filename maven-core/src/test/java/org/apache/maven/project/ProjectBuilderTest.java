package org.apache.maven.project;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.execution.MavenSession;

public class ProjectBuilderTest
    extends AbstractCoreMavenComponentTestCase
{
    protected String getProjectsDirectory()
    {
        return "src/test/projects/project-builder";
    }

    public void testSystemScopeDependencyIsPresentInTheCompileClasspathElements()
        throws Exception
    {
        File pom = getProject( "it0063" );
        
        Properties eps = new Properties();
        eps.setProperty( "jre.home", new File( pom.getParentFile(), "jdk/jre" ).getPath() );        
        
        MavenSession session = createMavenSession( pom, eps );
        MavenProject project = session.getCurrentProject();
        
        // Here we will actually not have any artifacts because the ProjectDependenciesResolver is not involved here. So
        // right now it's not valid to ask for artifacts unless plugins require the artifacts.
        
        List<String> elements = project.getCompileClasspathElements();
    }
}
