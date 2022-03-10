package org.apache.maven.internal.impl;

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

import javax.annotation.Nonnull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Metadata;
import org.apache.maven.api.services.ArtifactManager;

public class DefaultArtifactManager implements ArtifactManager
{
    private static final String SNAPSHOT = "SNAPSHOT";
    private static final Pattern SNAPSHOT_TIMESTAMP = Pattern.compile( "^(.*-)?([0-9]{8}\\.[0-9]{6}-[0-9]+)$" );


    private final Map<Artifact, Path> paths = new ConcurrentHashMap<>();
    private final Map<Artifact, Collection<Metadata>> metadatas = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public Optional<Path> getPath( @Nonnull Artifact artifact )
    {
        Path path = paths.get( artifact );
        return path != null ? Optional.of( path ) : artifact.getPath();
    }

    @Override
    public void setPath( @Nonnull Artifact artifact, Path path )
    {
        if ( path == null )
        {
            paths.remove( artifact );
        }
        else
        {
            paths.put( artifact, path );
        }
    }

    @Nonnull
    @Override
    public Collection<Metadata> getAttachedMetadatas( @Nonnull Artifact artifact )
    {
        Collection<Metadata> m = metadatas.get( artifact );
        return m != null ? Collections.unmodifiableCollection( m ) : Collections.emptyList();
    }

    @Override
    public void attachMetadata( @Nonnull Artifact artifact, @Nonnull Metadata metadata )
    {
        metadatas.computeIfAbsent( artifact, a -> new CopyOnWriteArrayList<>() ).add( metadata );
    }

    @Override
    public boolean isSnapshot( String version )
    {
        return version.endsWith( SNAPSHOT ) || SNAPSHOT_TIMESTAMP.matcher( version ).matches();
    }
}
