package org.apache.maven.plugin.coreit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Check that we correctly use the implementation parameter. See MNG-2293
 *
 * @goal param-implementation
 * @description Prints out the name of the implementation of the bla field.
 */
public class ParameterImplementationMojo
    extends AbstractMojo
{

    /**
     * @parameter implementation="org.apache.maven.plugin.coreit.sub.MyBla"
     * @required
     */
    private Bla bla;

    /**
     * The expected value of bla.toString().
     *
     * @parameter
     * @required
     */
    private String expected;

    public void execute()
        throws MojoExecutionException
    {

        getLog().info( "bla: " + bla );

        if ( ! expected.equals( bla.toString() ) )
        {
            throw new MojoExecutionException( "Expected '" + expected + "'; found '" + bla + "'" );
        }
    }

}
