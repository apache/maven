package mng3530;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.util.Iterator;
import java.util.List;

/**
 * Validate that the current project's {@link Resource} instances don't contain
 * uninterpolated expressions.
 *
 * @goal validate
 * @phase package
 */
public class ValidatePropertyMojo
    implements Mojo
{

    /**
     * @parameter default-value="${project.resources}"
     * @readonly
     */
    private List resources;

    private Log log;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        for ( Iterator it = resources.iterator(); it.hasNext(); )
        {
            Resource resource = (Resource) it.next();

            if ( resource.getDirectory().indexOf( "${project.build.directory}" ) > -1 )
            {
                throw new MojoExecutionException( "Project resource-directory was not interpolated.\n\nFull directory is: " + resource.getDirectory() );
            }
        }

        getLog().info( "Resource directory has been interpolated." );
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
