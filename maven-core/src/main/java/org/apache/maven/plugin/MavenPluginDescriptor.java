package org.apache.maven.plugin;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;

import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenPluginDescriptor
    extends ComponentSetDescriptor
{
    private PluginDescriptor pluginDescriptor;

    private List mojoDescriptors;

    public MavenPluginDescriptor( PluginDescriptor pluginDescriptor )
    {
        this.pluginDescriptor = pluginDescriptor;
    }

    public String getPluginId()
    {
        // TODO: groupID
        return pluginDescriptor.getArtifactId();
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public PluginDescriptor getPluginDescriptor()
    {
        return pluginDescriptor;
    }

    public List getMavenMojoDescriptors()
    {
        return super.getComponents();
    }

    public boolean isIsolatedRealm()
    {
        return pluginDescriptor.isIsolatedRealm();
    }
}
