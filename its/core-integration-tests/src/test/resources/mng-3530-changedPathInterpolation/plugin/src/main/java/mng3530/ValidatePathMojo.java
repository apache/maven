package mng3530;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Validate that the project paths have been reset by the {@link SetPathMojo}.
 *
 * @goal validate
 * @phase package
 */
public class ValidatePathMojo
    implements Mojo
{

    /**
     * @parameter
     */
    private File buildDirectory;

    /**
     * Project instance to validate.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private Log log;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Validating buildDirectory parameter: " + buildDirectory );
        if ( !project.getBuild().getDirectory().equals( buildDirectory.getAbsolutePath() ) )
        {
            throw new MojoExecutionException( "buildDirectory parameter value:\n\n" + buildDirectory
                                              + "\n\ndoes not match project.build.directory:\n\n"
                                              + project.getBuild().getDirectory() + "\n" );
        }
        else if ( !buildDirectory.getName().equals( SetPathMojo.MODIFIED_BUILD_DIRECTORY_NAME ) )
        {
            throw new MojoExecutionException( "buildDirectory parameter value:\n\n" + buildDirectory
                                              + "\n\ndoes not use modified target dir-name: "
                                              + SetPathMojo.MODIFIED_BUILD_DIRECTORY_NAME + "\n" );
        }
        else
        {
            getLog().info( "buildDirectory matches project.build.directory, and points to modified target location." );
        }
    }

    public Log getLog()
    {
        return log;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

}
