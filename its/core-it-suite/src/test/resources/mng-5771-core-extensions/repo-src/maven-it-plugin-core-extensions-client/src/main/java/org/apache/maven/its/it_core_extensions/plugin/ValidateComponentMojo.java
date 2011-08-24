package org.apache.maven.its.it_core_extensions.plugin;

import java.util.Map;

import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo( name = "validate-component" )
public class ValidateComponentMojo
    extends AbstractMojo
{
    @Component
    private Map<String, ClassRealmManagerDelegate> delegates;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ClassRealmManagerDelegate delegate = delegates.get( "TestClassRealmManagerDelegate" );
        if ( delegate == null )
        {
            throw new MojoExecutionException( "Expected core extension component is not available" );
        }
    }
}
