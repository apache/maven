package org.apache.maven.plugin.coreit;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.ssh.jsch.ScpWagon;

/**
 * @goal use-wagon
 * @phase validate
 */
public class UsesWagonMojo
    extends AbstractMojo
{
    /**
     * @component
     */
    private WagonManager wagonManager;
    
    public void execute()
        throws MojoExecutionException
    {
        Object fileWagon;
        try
        {
            fileWagon = wagonManager.getWagon( "file" );
        }
        catch( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        try
        {
            FileWagon theWagon = (FileWagon) fileWagon;
        }
        catch( ClassCastException e )
        {
            getLog().error( "", e );
            getLog().error( "Plugin Class Loaded by " + FileWagon.class.getClassLoader() );
            getLog().error( "Wagon Class Loaded by " + fileWagon.getClass().getClassLoader() );

            throw e;
        }

        Object scpWagon;
        try
        {
            scpWagon = wagonManager.getWagon( "scp" );
        }
        catch( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        try
        {
            ScpWagon theWagon = (ScpWagon) scpWagon;
        }
        catch( ClassCastException e )
        {
            getLog().error( "", e );
            getLog().error( "Plugin Class Loaded by " + ScpWagon.class.getClassLoader() );
            getLog().error( "Wagon Class Loaded by " + scpWagon.getClass().getClassLoader() );

            throw e;
        }
    }

}
