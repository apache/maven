package org.apache.maven.lifecycle.binding;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.plugin.loader.PluginPrefixLoader;
import org.apache.maven.project.MavenProject;

import java.util.StringTokenizer;

/**
 * Responsible for constructing or parsing MojoBinding instances from one of several sources, potentially
 * using the {@link PluginLoader} to resolve any plugin prefixes first.
 *
 * @author jdcasey
 *
 */
public class DefaultMojoBindingFactory
    implements MojoBindingFactory
{

    PluginPrefixLoader pluginPrefixLoader;

    /**
     * Parse the specified mojo string into a MojoBinding, optionally allowing plugin-prefix references.
     * If a plugin-prefix is allowed and used, resolve the prefix and use the resulting PluginDescriptor
     * to set groupId and artifactId on the MojoBinding instance.
     */
    public MojoBinding parseMojoBinding( String bindingSpec, MavenProject project, MavenSession session, boolean allowPrefixReference )
        throws LifecycleSpecificationException, LifecycleLoaderException
    {
        StringTokenizer tok = new StringTokenizer( bindingSpec, ":" );
        int numTokens = tok.countTokens();

        MojoBinding binding = null;

        if ( numTokens == 2 )
        {
            if ( !allowPrefixReference )
            {
                String msg = "Mapped-prefix lookup of mojos are only supported from direct invocation. "
                    + "Please use specification of the form groupId:artifactId[:version]:goal instead.";

                throw new LifecycleSpecificationException( msg );
            }

            String prefix = tok.nextToken();

            Plugin plugin;
            try
            {
                plugin = pluginPrefixLoader.findPluginForPrefix( prefix, project, session );
            }
            catch ( PluginLoaderException e )
            {
                throw new LifecycleLoaderException(
                                                    "Failed to find plugin for prefix: " + prefix + ". Reason: " + e.getMessage(),
                                                    e );
            }

            binding = createMojoBinding( plugin.getGroupId(), plugin.getArtifactId(),
                                         plugin.getVersion(), tok.nextToken(), project );
        }
        else if ( ( numTokens == 3 ) || ( numTokens == 4 ) )
        {
            binding = new MojoBinding();

            String groupId = tok.nextToken();
            String artifactId = tok.nextToken();

            String version = null;
            if ( numTokens == 4 )
            {
                version = tok.nextToken();
            }

            String goal = tok.nextToken();

            binding = createMojoBinding( groupId, artifactId, version, goal, project );
        }
        else
        {
            String message = "Invalid task '" + bindingSpec + "': you must specify a valid lifecycle phase, or"
                + " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";

            throw new LifecycleSpecificationException( message );
        }

        return binding;
    }

    /**
     * Create a new MojoBinding instance with the specified information, and inject POM configurations
     * appropriate to that mojo before returning it.
     */
    public MojoBinding createMojoBinding( String groupId, String artifactId, String version, String goal, MavenProject project )
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( groupId );
        binding.setArtifactId( artifactId );
        binding.setVersion( version );
        binding.setGoal( goal );

        BindingUtils.injectProjectConfiguration( binding, project );

        return binding;
    }

    /**
     * Simplified version of {@link MojoBindingFactory#parseMojoBinding(String, MavenProject, boolean)}
     * which assumes the project is null and prefixes are not allowed. This method will <b>never</b>
     * result in the {@link PluginLoader} being used to resolve the PluginDescriptor.
     */
    public MojoBinding parseMojoBinding( String bindingSpec )
        throws LifecycleSpecificationException
    {
        try
        {
            return parseMojoBinding( bindingSpec, null, null, false );
        }
        catch ( LifecycleLoaderException e )
        {
            IllegalStateException error = new IllegalStateException( e.getMessage()
                + "\n\nTHIS SHOULD BE IMPOSSIBLE DUE TO THE USAGE OF THE PLUGIN-LOADER." );

            error.initCause( e );

            throw error;
        }
    }

    public MojoBinding parseMojoBinding( String bindingSpec,
                                         MavenProject project )
        throws LifecycleSpecificationException
    {
        try
        {
            return parseMojoBinding( bindingSpec, project, null, false );
        }
        catch ( LifecycleLoaderException e )
        {
            IllegalStateException error = new IllegalStateException( e.getMessage()
                + "\n\nTHIS SHOULD BE IMPOSSIBLE DUE TO THE USAGE OF THE PLUGIN-LOADER." );

            error.initCause( e );

            throw error;
        }
    }

}
