package org.apache.maven.plugin.coreit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal loadable
 * @requiresDependencyResolution test
 */
public class LoadableMojo
    extends AbstractMojo
{
    /**
     * @parameter
     * @required
     */
    private String className;

    public void execute() throws MojoFailureException
    {
        if ( !load( true ) || !load( false ) )
        {
            throw new MojoFailureException( this, "Class-loading test failed..", "Failed to load class: " + className + " using one or more methods." );
        }
    }
    
    private boolean load( boolean useContextClassloader ) throws MojoFailureException
    {
        getLog().info( "Executing in java version: " + System.getProperty( "java.version" ) );
        
        ClassLoader cl;
        if ( useContextClassloader )
        {
            cl = Thread.currentThread().getContextClassLoader();
        }
        else
        {
            cl = this.getClass().getClassLoader();
        }

        getLog().info( "Attepting to load: " + className + " from: " + cl + (useContextClassloader ? " (context classloader)" : "" ) );
        
        try
        {
            Class result = cl.loadClass( className );
            
            getLog().info( "Load succeeded." );
            
            return true;
        }
        catch ( ClassNotFoundException e )
        {
            getLog().info( "Failed to load class: " + className
                + (useContextClassloader ? " using context classloader" : "") );
            
            return false;
        }
    }
}
