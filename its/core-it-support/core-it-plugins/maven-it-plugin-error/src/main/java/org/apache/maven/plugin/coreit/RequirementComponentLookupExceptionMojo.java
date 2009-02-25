package org.apache.maven.plugin.coreit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal requirement-component-lookup-exception
 * @requiresProject false
 */
public class RequirementComponentLookupExceptionMojo
    extends AbstractMojo
{
    
    /**
     * @component role="missing-component" roleHint="triggers-error"
     */
    private Mojo dependency;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        dependency.execute();
    }

}
