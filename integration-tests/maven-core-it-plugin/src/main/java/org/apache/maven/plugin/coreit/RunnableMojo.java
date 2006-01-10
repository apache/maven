package org.apache.maven.plugin.coreit;

import bsh.EvalError;
import bsh.Interpreter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal runnable
 * @requiresDependencyResolution test
 */
public class RunnableMojo
    extends AbstractMojo
{
    /**
     * @parameter
     * @required
     */
    private String script;

    public void execute() throws MojoExecutionException
    {
        Interpreter terp = new Interpreter();

        try
        {
            getLog().info( "Executing in java version: " + System.getProperty( "java.version" ) );
            
            Object result = terp.eval( script );

            getLog().info( "Result of script evaluation was: " + result + "\nLoaded from: " + result.getClass().getClassLoader() );
        }
        catch ( EvalError e )
        {
            throw new MojoExecutionException( "Failed to evaluate script.", e );
        }
    }
}
