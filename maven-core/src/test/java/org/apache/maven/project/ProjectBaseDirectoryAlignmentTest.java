package org.apache.maven.project;

import org.apache.maven.MavenTestCase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Resource;

import java.io.File;
import java.util.Iterator;

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

        assertTrue( project.getBuild().getUnitTestSourceDirectory().startsWith( getBasedir() ) );

        Build build = project.getBuild();

        Resource resource = (Resource) build.getResources().get( 0 );

        assertTrue( resource.getDirectory().startsWith( getBasedir() ) );

        String relativeFile = null;
        String absoluteFile = null;
        String managedDependencyFile = null;
        for ( Iterator i = project.getDependencies().iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();
            if ( d.getGroupId().equals( "g1" ) )
            {
                relativeFile = d.getFile();
            }
            else if ( d.getGroupId().equals( "g2" ) )
            {
                absoluteFile = d.getFile();
            }
            else if ( d.getGroupId().equals( "g3" ) )
            {
                managedDependencyFile = d.getFile();
            }
        }

        assertNotNull( "Required dependency missing: g1:d1", relativeFile );
        assertNotNull( "Required dependency missing: g2:d2", absoluteFile );
        assertNotNull( "Required dependency missing: g3:d3", managedDependencyFile );

        assertTrue( "Relative file not adjusted", relativeFile.startsWith( getBasedir() ) );
        assertEquals( "Absolute file was incorrectly modified", "/top/level/path", absoluteFile );
        assertTrue( "Managed dependency not adjusted", managedDependencyFile.startsWith( getBasedir() ) );
    }
}
