package org.apache.maven.plugin.coreit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import junit.framework.TestCase;

/**
 * Takes a parameter with a class from a provided-scope dependency, so that the mojo class itself won't be
 * loadable when that dependency is missing (in the runtime environment).
 * 
 * @goal no-class-def-found-error-param
 * @requiresProject false
 */
public class NoClassDefFoundErrorComponentMojo
    extends AbstractMojo
{
    
    /**
     * @parameter default-value="foo"
     */
    private TestCase value;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        value.getName();
    }

}
