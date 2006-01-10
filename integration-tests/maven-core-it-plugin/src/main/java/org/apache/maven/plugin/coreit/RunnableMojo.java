package org.apache.maven.plugin.coreit;

import bsh.EvalError;
import bsh.Interpreter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;

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

    public void execute() throws MojoFailureException
    {
        Interpreter terp = new Interpreter();

        try
        {
            getLog().info( "Executing in java version: " + System.getProperty( "java.version" ) );
            
            Class result = (Class) terp.eval( script );

            getLog().info( "Result of script evaluation was: " + result + "\nLoaded from: " + result.getClassLoader() );
        }
        catch ( EvalError e )
        {
            throw new MojoFailureException( this, "Failed to evaluate script.", "Script: \n\n" + script
                + "\n\nfailed to evaluate. Error: " + e.getMessage() + "\nLine: " + e.getErrorLineNumber() );
        }
    }
}
