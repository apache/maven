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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Caches raw plugin descriptors. A raw plugin descriptor is a descriptor that has just been extracted from the plugin
 * artifact and does not contain any runtime specific data. The cache must not be used for descriptors that hold runtime
 * data like the plugin realm. <strong>Warning:</strong> This is an internal utility interface that is only public for
 * technical reasons, it is not part of the public API. In particular, this interface can be changed or deleted without
 * prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
public interface PluginDescriptorCache
{

    /**
     * A cache key.
     */
    interface Key
    {
        // marker interface for cache keys
    }

    Key createKey( Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session );

    void put( Key key, PluginDescriptor pluginDescriptor );

    PluginDescriptor get( Key key );

    void flush();

}
