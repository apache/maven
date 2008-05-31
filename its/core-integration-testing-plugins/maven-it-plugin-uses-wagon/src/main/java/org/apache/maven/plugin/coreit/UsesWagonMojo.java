package org.apache.maven.plugin.coreit;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.Wagon;
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
        try
        {
            Wagon wagon = wagonManager.getWagon( "scp" );

            ScpWagon myWagon = (ScpWagon) wagon;
        }
        catch( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
