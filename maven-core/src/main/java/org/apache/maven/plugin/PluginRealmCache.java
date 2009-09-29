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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * Caches plugin class realms. <strong>Warning:</strong> This is an internal utility interface that is only public for
 * technical reasons, it is not part of the public API. In particular, this interface can be changed or deleted without
 * prior notice.
 * 
 * @author Igor Fedorenko
 * @author Benjamin Bentmann
 */
public interface PluginRealmCache
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

    CacheRecord get( Plugin plugin, ClassLoader parentRealm, List<String> parentImports,
                     ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories );

    CacheRecord put( Plugin plugin, ClassLoader parentRealm, List<String> parentImports,
                     ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                     ClassRealm pluginRealm, List<Artifact> pluginArtifacts );

    void flush();

    /**
     * Registers the specified cache record for usage with the given project. Integrators can use the information
     * collected from this method in combination with a custom cache implementation to dispose unused records from the
     * cache.
     * 
     * @param project The project that employs the plugin realm, must not be {@code null}.
     * @param record The cache record being used for the project, must not be {@code null}.
     */
    void register( MavenProject project, CacheRecord record );

}
