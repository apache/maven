package org.apache.maven.model.converter.relocators;

import java.util.Collection;

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

/**
 * A manager for plugin relocators.
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public interface PluginRelocatorManager
{
    String ROLE = PluginRelocatorManager.class.getName();

    /**
     * Get a named plugin relocator.
     *
     * @param pluginRelocatorId The role-hint for the plexus component
     * @return The named plugin relocator
     * @throws NoSuchPluginRelocatorException If the named plugin relocator can not be found
     */
    PluginRelocator getPluginRelocator( String pluginRelocatorId )
        throws NoSuchPluginRelocatorException;

    /**
     * Get all available plugin relocators.
     *
     * @return A <code>Collection</code> of <code>PluginRelocator</code> objects
     */
    Collection getPluginRelocators();
}
