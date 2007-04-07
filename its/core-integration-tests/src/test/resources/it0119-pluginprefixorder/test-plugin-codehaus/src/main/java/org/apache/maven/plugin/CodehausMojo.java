package org.apache.maven.plugin;

import java.net.URL;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @goal codehaus
 */
public class CodehausMojo
    extends AbstractMojo
{

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        this.getLog().info("Ran Test Codehaus");
    }

}
