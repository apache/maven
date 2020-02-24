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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.ExtensionDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;

/**
 * Default extension realm cache implementation. Assumes cached data does not change.
 */
@Component( role = ExtensionRealmCache.class )
public class DefaultExtensionRealmCache
    implements ExtensionRealmCache, Disposable
{

    /**
     * Control flag to restore legacy filesystem behaviour in case issues.
     */
    private final boolean useFsKey = Boolean.getBoolean( "maven.extensions.fskey" );

    /**
     * Legacy key implementation which access file system each time just to create a key. As this operation is invoked
     * under a lock it slows down graph build significantly on slow file systems. Initial reason for this implementation
     * is unknown, but we can assume it is a sort of protection from background file modifications at build time.
     * Though such protection itself raises questions of build correctness if maven coordinates map to physically
     * different filesets during the same build.
     *
     * @deprecated use {@link ArtifactCacheKey}
     * @see #createKey(List)
     */
    protected static class FileCacheKey
        implements Key
    {

        private final List<FileInfo> files;

        private final int hashCode;

        private FileCacheKey( List<Artifact> extensionArtifacts )
        {
            this.files = extensionArtifacts.stream().map( artifact -> {
                File file = artifact.getFile();
                long lastModified = file != null ? file.lastModified() : 0;
                long size = file != null ? file.length() : 0;
                return new FileInfo( file, lastModified, size, artifact.getVersion() );
            } ).collect( Collectors.toCollection( ArrayList::new ) );

            this.hashCode = files.hashCode();
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( o == this )
            {
                return true;
            }

            if ( !( o instanceof FileCacheKey ) )
            {
                return false;
            }

            FileCacheKey other = (FileCacheKey) o;

            return files.equals( other.files );
        }

        @Override
        public String toString()
        {
            return files.toString();
        }
    }

    /**
     * Lightweight artifact coordinates based key
     * @see #createKey(List)
     */
    protected static class ArtifactCacheKey
            implements Key
    {

        private final List<String> artifactKeys;

        private final int hashCode;

        private ArtifactCacheKey( List<Artifact> extensionArtifacts )
        {
            this.artifactKeys = extensionArtifacts.stream().map( ArtifactCacheKey::getArtifactKey )
                    .collect( Collectors.toCollection( ArrayList::new ) );
            this.hashCode = artifactKeys.hashCode();
        }

        private static String getArtifactKey( Artifact artifact )
        {
            return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion()
                    + ( artifact.hasClassifier() ? ":" + artifact.getClassifier() : "" ) + ":" + artifact.getType();
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( o == this )
            {
                return true;
            }

            if ( !( o instanceof ArtifactCacheKey ) )
            {
                return false;
            }

            ArtifactCacheKey other = (ArtifactCacheKey) o;

            return artifactKeys.equals( other.artifactKeys );
        }

        @Override
        public String toString()
        {
            return artifactKeys.toString();
        }
    }

    protected final Map<Key, CacheRecord> cache = new ConcurrentHashMap<>();

    @Override
    public Key createKey( List<Artifact> extensionArtifacts )
    {
        return useFsKey ? new FileCacheKey( extensionArtifacts ) : new ArtifactCacheKey( extensionArtifacts );
    }

    public CacheRecord get( Key key )
    {
        return cache.get( key );
    }

    public CacheRecord put( Key key, ClassRealm extensionRealm, ExtensionDescriptor extensionDescriptor,
                            List<Artifact> artifacts )
    {
        Objects.requireNonNull( extensionRealm, "extensionRealm cannot be null" );

        if ( cache.containsKey( key ) )
        {
            throw new IllegalStateException( "Duplicate extension realm for extension " + key );
        }

        CacheRecord record = new CacheRecord( extensionRealm, extensionDescriptor, artifacts );

        cache.put( key, record );

        return record;
    }

    public void flush()
    {
        for ( CacheRecord record : cache.values() )
        {
            ClassRealm realm = record.getRealm();
            try
            {
                realm.getWorld().disposeRealm( realm.getId() );
            }
            catch ( NoSuchRealmException e )
            {
                // ignore
            }
        }
        cache.clear();
    }

    public void register( MavenProject project, Key key, CacheRecord record )
    {
        // default cache does not track extension usage
    }

    public void dispose()
    {
        flush();
    }

    private static class FileInfo
    {
        private final File file;
        private final Long lastModified;
        private final Long size;
        private final String version;

        FileInfo( File file, long lastModified, long size, String version )
        {
            this.file = file;
            this.lastModified = lastModified;
            this.size = size;
            this.version = version;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }
            FileInfo fileInfo = (FileInfo) o;
            return Objects.equals( file, fileInfo.file ) && lastModified.equals( fileInfo.lastModified ) && size.equals(
                    fileInfo.size ) && Objects.equals( version, fileInfo.version );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( file, lastModified, size, version );
        }

        @Override
        public String toString()
        {
            return String.valueOf( file );
        }
    }
}
