package org.apache.maven.plugin.coreit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * "Throw" a parameter into the plugin context, for the "catch" mojo to 
 * pickup and process.
 * 
 * @goal throw
 */
public class ThrowMojo
    extends AbstractMojo
{
    
    public static final String THROWN_PARAMETER = "throw-parameter";

    /**
     * @parameter expression="${value}" default-value="thrown"
     */
    private String value;
    
    public void setValue( String value )
    {
        this.value = value;
    }
    
    public String getValue()
    {
        return value;
    }
    
    public void execute()
        throws MojoExecutionException
    {
        getPluginContext().put( THROWN_PARAMETER, value );
    }

}
