package org.apache.maven.its.it_core_extensions.plugin;

import org.apache.maven.its.core_extensions.TestCoreExtensionComponent;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo( name = "validate-classpath" )
public class ValidateClasspathMojo
    extends AbstractMojo
{
    @Component
    private TestCoreExtensionComponent component;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( this.component == null )
        {
            throw new MojoExecutionException( "Expected core extension component is not available" );
        }
    }

}
