package org.apache.maven.plugin.coreit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;

import java.net.URL;

/**
 * @goal reachable
 * @requiresDependencyResolution test
 */
public class ReachableMojo extends AbstractMojo
{
    /**
     * @parameter
     * @required
     */
    private String resource;
    
    public void execute()
    throws MojoFailureException
    {
        if ( !reach( true ) || !reach( false ) )
        {
            throw new MojoFailureException( this, "Resource reachability test failed..", "Failed to reach resource: " + resource + " using one or more methods." );
        }
    }
    
    public boolean reach( boolean useContextClassloader ) throws MojoFailureException
    {
        ClassLoader cl;
        if ( useContextClassloader )
        {
            cl = Thread.currentThread().getContextClassLoader();
        }
        else
        {
            cl = this.getClass().getClassLoader();
        }
        
        URL result = cl.getResource( resource );
        
        getLog().info( "Attepting to reach: " + resource + " from: " + cl + (useContextClassloader ? " (context classloader)" : "" ) + ( result == null ? ": FAILED" : ":SUCCEEDED" ) );
        
        if ( result == null )
        {
            getLog().info( "Cannot find resource: " + resource + (useContextClassloader?" in context classloader":"") );
            
            return false;
        }
        
        return true;
    }
}
