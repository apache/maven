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

    private static final ConcurrentHashMap<String, FileInfo> FILE_INFO_CACHE = new ConcurrentHashMap<>( 512 );

    /**
     * CacheKey
     */
    protected static class CacheKey
        implements Key
    {

        private final List<FileInfo> files;

        private final int hashCode;

        public CacheKey( List<Artifact> extensionArtifacts )
        {
            this.files = new ArrayList<>( extensionArtifacts.size() );

            for ( Artifact artifact : extensionArtifacts )
            {
                String artifactKey = getArtifactKey( artifact );
                FileInfo fileInfo = FILE_INFO_CACHE.get( artifactKey );
                if ( fileInfo == null )
                {
                    File file = artifact.getFile();
                    long lastModified = file != null ? file.lastModified() : 0;
                    long size = file != null ? file.length() : 0;
                    fileInfo = new FileInfo( file, lastModified, size, artifact.getVersion() );
                    FILE_INFO_CACHE.putIfAbsent( artifactKey, fileInfo );
                }
                files.add( fileInfo );
            }

            this.hashCode = files.hashCode();
        }

        private String getArtifactKey( Artifact artifact )
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

            if ( !( o instanceof CacheKey ) )
            {
                return false;
            }

            CacheKey other = (CacheKey) o;

            return files.equals( other.files );
        }

        @Override
        public String toString()
        {
            return files.toString();
        }
    }

    protected final Map<Key, CacheRecord> cache = new ConcurrentHashMap<>();

    @Override
    public Key createKey( List<Artifact> extensionArtifacts )
    {
        return new CacheKey( extensionArtifacts );
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
