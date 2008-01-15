package org.apache.maven.plugin;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.realm.RealmManagementException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.component.repository.exception.ComponentRepositoryException;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

/**
 * Exception which occurs to indicate that the plugin cannot be initialized due
 * to some deeper problem with Plexus. Context information includes the groupId,
 * artifactId, and version for the plugin; at times, the goal name for which
 * execution failed; a message detailing the problem; the ClassRealm used to
 * lookup the plugin; and the Plexus exception that caused this error.
 *
 * @author jdcasey
 *
 */
public class PluginContainerException
    extends PluginManagerException
{

    private ClassRealm pluginRealm;

    public PluginContainerException( MojoDescriptor mojoDescriptor,
                                     ClassRealm pluginRealm,
                                     String message,
                                     ComponentLookupException e )
    {
        super( mojoDescriptor, message, e );

        this.pluginRealm = pluginRealm;
    }

    public PluginContainerException( Plugin plugin,
                                     ClassRealm pluginRealm,
                                     String message,
                                     PlexusConfigurationException e )
    {
        super( plugin, message, e );

        this.pluginRealm = pluginRealm;
    }

    public PluginContainerException( Plugin plugin,
                                     ClassRealm pluginRealm,
                                     String message,
                                     ComponentRepositoryException e )
    {
        super( plugin, message, e );

        this.pluginRealm = pluginRealm;
    }

    public PluginContainerException( Plugin plugin,
                                     String message,
                                     RealmManagementException e )
    {
        super( plugin, message, e );
    }

    public ClassRealm getPluginRealm()
    {
        return pluginRealm;
    }
}
