package org.apache.maven.its.plugin;

import org.apache.maven.its.extensions.TestSessionScopedComponent;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo( name = "validate-session-scoped" )
public class ValidateSessionScopedMojo
    extends AbstractMojo
{
    @Component
    private TestSessionScopedComponent component;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( this.component == null )
        {
            throw new MojoExecutionException( "Expected core extension component is not available" );
        }
    }

}
