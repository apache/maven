package org.apache.maven.project;

import org.apache.maven.MavenTestCase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;

import java.io.File;

public class ProjectBaseDirectoryAlignmentTest
    extends MavenTestCase
{

    private String dir = "src/test/resources/projects/base-directory-alignment/";

    public void testProjectDirectoryBaseDirectoryAlignment()
        throws Exception
    {
        File f = getTestFile( dir + "project-which-needs-directory-alignment.xml" );

        MavenProject project = getProject( f, false );

        assertNotNull( "Test project can't be null!", project );

        assertTrue( project.getBuild().getSourceDirectory().startsWith( getBasedir() ) );

        assertTrue( project.getBuild().getTestSourceDirectory().startsWith( getBasedir() ) );

        Build build = project.getBuild();

        Resource resource = (Resource) build.getResources().get( 0 );

        assertTrue( resource.getDirectory().startsWith( getBasedir() ) );
    }
}
