package org.apache.maven.plugin;

import org.apache.maven.plugin.descriptor.Dependency;
import org.codehaus.plexus.component.repository.ComponentDependency;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenPluginDependency
    extends ComponentDependency
{
    public MavenPluginDependency( Dependency dependency )
    {
        setGroupId( dependency.getGroupId() );

        setArtifactId( dependency.getArtifactId() );

        setType( dependency.getType() );

        setVersion( dependency.getVersion() );
    }
}
