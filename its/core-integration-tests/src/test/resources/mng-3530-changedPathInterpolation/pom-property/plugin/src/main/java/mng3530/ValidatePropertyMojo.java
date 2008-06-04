package mng3530;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Validate that the POM property has been reset by the {@link SetPropertyMojo}.
 *
 * @goal validate
 * @phase package
 */
public class ValidatePropertyMojo
    implements Mojo
{

    /**
     * @parameter
     */
    private String buildDirectory;

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
        String apiValue = project.getProperties().getProperty( "myDirectory" );

        if ( !apiValue.equals( buildDirectory ) )
        {
            throw new MojoExecutionException( "buildDirectory parameter value:\n\n" + buildDirectory
                                              + "\n\ndoes not match ${myDirectory} from project:\n\n"
                                              + apiValue + "\n" );
        }
        else
        {
            getLog().info( "buildDirectory matches ${myDirectory} in the current POM instance." );
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
