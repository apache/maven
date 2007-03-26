package org.apache.maven.plugin;

import java.net.URL;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @goal test
 * @phase verify
 */
public class It0014Mojo
    extends AbstractMojo
{

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ClassLoader cl = this.getClass().getClassLoader();
        URL url = cl.getResource( "it0114_rule_set.xml" );
        
        if (url != null)
        {
            this.getLog().info( "Found Url: "+ url.getFile() );
        }
        else
        {
            throw new MojoExecutionException("Can't find it0114_rule_set.xml on classpath!!");
        }
        
    }

}
