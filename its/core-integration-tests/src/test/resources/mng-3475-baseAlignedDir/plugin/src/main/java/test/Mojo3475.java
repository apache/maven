package test;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Build;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @goal validate
 * @phase validate
 */
public class Mojo3475
    extends AbstractMojo
{
    /**
    * @parameter expression="${project}"
    */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Build build = project.getModel().getBuild();
        List failed = new ArrayList();
        
        testDirectoryAbsolutePath( build.getDirectory(), failed );
        testDirectoryAbsolutePath( build.getOutputDirectory(), failed );
        testDirectoryAbsolutePath( build.getTestOutputDirectory(), failed );
        testDirectoryAbsolutePath( build.getSourceDirectory(), failed );
        testDirectoryAbsolutePath( build.getTestSourceDirectory(), failed );

        // MNG-3741: Don't worry about relative script source directory.
        // testDirectoryAbsolutePath( build.getScriptSourceDirectory(), failed );

        testDirectoryAbsolutePath( project.getReporting().getOutputDirectory(), failed );
        
        if ( !failed.isEmpty() )
        {
            throw new MojoExecutionException( "One or more directories were not absolute:\n\n" + StringUtils.join( failed.iterator(), "\n" ) );
        }
    }

    private void testDirectoryAbsolutePath( String directory, List failed )
        throws MojoExecutionException
    {
        if ( !new File( directory ).isAbsolute() )
        {
            failed.add( directory );
        }
        
        System.out.println( directory );
    }
}
