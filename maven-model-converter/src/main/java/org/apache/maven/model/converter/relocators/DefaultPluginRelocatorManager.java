package org.apache.maven.model.converter.relocators;

/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.Collection;
import java.util.Map;

/**
 * A default implementation of the <code>PluginRelocatorManager</code> interface.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @plexus.component role="org.apache.maven.model.converter.relocators.PluginRelocatorManager"
 */
public class DefaultPluginRelocatorManager extends AbstractLogEnabled implements PluginRelocatorManager
{
    /**
     * @plexus.requirement role="org.apache.maven.model.converter.relocators.PluginRelocator"
     */
    private Map pluginRelocators;

    public PluginRelocator getPluginRelocator( String pluginRelocatorId )
        throws NoSuchPluginRelocatorException
    {
        PluginRelocator pluginRelocator = (PluginRelocator) pluginRelocators.get( pluginRelocatorId );

        if ( pluginRelocator == null )
        {
            throw new NoSuchPluginRelocatorException( pluginRelocatorId );
        }

        return pluginRelocator;
    }

    public Collection getPluginRelocators()
    {
        return pluginRelocators.values();
    }
}
