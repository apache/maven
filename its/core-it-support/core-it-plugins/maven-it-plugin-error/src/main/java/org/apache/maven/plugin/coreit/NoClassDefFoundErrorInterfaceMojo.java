package org.apache.maven.plugin.coreit;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import junit.framework.TestCase;

/**
 * Implements a class from a provided-scope dependency, so that the mojo class itself won't be
 * loadable when that dependency is missing (in the runtime environment).
 * 
 * @goal no-class-def-found-error-mojo
 * @requiresProject false
 */
public class NoClassDefFoundErrorInterfaceMojo
    extends TestCase
    implements Mojo
{
    
    private Log log;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
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
