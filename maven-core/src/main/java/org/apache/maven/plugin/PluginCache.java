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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

public interface PluginCache
{
    
    public static class CacheRecord
    {
        public final ClassRealm realm;
        public final List<Artifact> artifacts;

        public CacheRecord( ClassRealm realm, List<Artifact> artifacts )
        {
            this.realm = realm;
            this.artifacts = artifacts;
        }
    }

    PluginDescriptor getPluginDescriptor( Plugin plugin, ArtifactRepository localRepository,
                                          List<ArtifactRepository> remoteRepositories );

    void putPluginDescriptor( Plugin plugin, ArtifactRepository localRepository,
                              List<ArtifactRepository> remoteRepositories, PluginDescriptor pluginDescriptor );

    CacheRecord get( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories );

    void put( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
              ClassRealm pluginRealm, List<Artifact> pluginArtifacts );

    void flush();
}
