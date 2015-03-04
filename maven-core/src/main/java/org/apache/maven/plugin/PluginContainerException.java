package org.apache.maven.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
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

    public PluginContainerException( MojoDescriptor mojoDescriptor, ClassRealm pluginRealm, String message,
                                     Throwable e )
    {
        super( mojoDescriptor, message, e );

        this.pluginRealm = pluginRealm;
    }

    public PluginContainerException( MojoDescriptor mojoDescriptor, ClassRealm pluginRealm, String message,
                                     ComponentLookupException e )
    {
        super( mojoDescriptor, message, e );

        this.pluginRealm = pluginRealm;
    }

    public PluginContainerException( Plugin plugin, ClassRealm pluginRealm, String message, Throwable e )
    {
        super( plugin, message, e );

        this.pluginRealm = pluginRealm;
    }

    public PluginContainerException( Plugin plugin, ClassRealm pluginRealm, String message,
                                     PlexusConfigurationException e )
    {
        super( plugin, message, e );

        this.pluginRealm = pluginRealm;
    }

    public PluginContainerException( Plugin plugin, ClassRealm pluginRealm, String message,
                                     ComponentRepositoryException e )
    {
        super( plugin, message, e );

        this.pluginRealm = pluginRealm;
    }

    public ClassRealm getPluginRealm()
    {
        return pluginRealm;
    }
}
