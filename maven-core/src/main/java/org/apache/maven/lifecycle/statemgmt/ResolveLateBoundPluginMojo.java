package org.apache.maven.lifecycle.statemgmt;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MojoBindingUtils;
import org.apache.maven.lifecycle.binding.MojoBindingFactory;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;

public class ResolveLateBoundPluginMojo extends AbstractMojo
{

    /**
     * @component
     */
    private PluginLoader pluginLoader;

    private String groupId;

    private String artifactId;

    private String version;

    private String goal;

    private MavenProject project;

    private MavenSession session;

    private MojoBindingFactory bindingFactory;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        MojoBinding binding = bindingFactory.createMojoBinding( groupId, artifactId, version, artifactId, project );
        try
        {
            PluginDescriptor descriptor = pluginLoader.loadPlugin( binding, project, session );

            MojoDescriptor mojoDescriptor = descriptor.getMojo( goal );

            if ( mojoDescriptor == null )
            {
                throw new MojoExecutionException( "Resolved plugin: " + descriptor.getId()
                                + " does not contain a mojo called \'" + goal + "\'." );
            }
        }
        catch ( PluginLoaderException e )
        {
            throw new MojoExecutionException( "Failed to load late-bound plugin: "
                            + MojoBindingUtils.createPluginKey( binding ), e );
        }
    }

}
