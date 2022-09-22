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

import java.util.Objects;

import org.apache.maven.api.Coordinate;
import org.apache.maven.api.Type;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.TypeRegistry;
import org.eclipse.aether.artifact.ArtifactProperties;

import static org.apache.maven.internal.impl.Utils.nonNull;

/**
 * A wrapper class around a maven resolver artifact.
 */
public class DefaultCoordinate implements Coordinate
{
    private final @Nonnull AbstractSession session;
    private final @Nonnull org.eclipse.aether.artifact.Artifact coordinate;

    public DefaultCoordinate( @Nonnull AbstractSession session,
                              @Nonnull org.eclipse.aether.artifact.Artifact coordinate )
    {
        this.session = nonNull( session, "session can not be null" );
        this.coordinate = nonNull( coordinate, "coordinate can not be null" );
    }

    public org.eclipse.aether.artifact.Artifact getCoordinate()
    {
        return coordinate;
    }

    @Nonnull
    @Override
    public String getGroupId()
    {
        return coordinate.getGroupId();
    }

    @Nonnull
    @Override
    public String getArtifactId()
    {
        return coordinate.getArtifactId();
    }

    @Nonnull
    @Override
    public VersionRange getVersion()
    {
        return session.parseVersionRange( coordinate.getVersion() );
    }

    @Override
    public String getExtension()
    {
        return coordinate.getExtension();
    }

    @Override
    public Type getType()
    {
        String type = coordinate.getProperty( ArtifactProperties.TYPE, coordinate.getExtension() );
        return session.getService( TypeRegistry.class ).getType( type );
    }

    @Nonnull
    @Override
    public String getClassifier()
    {
        return coordinate.getClassifier();
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
        DefaultCoordinate that = (DefaultCoordinate) o;
        return Objects.equals( this.getGroupId(), that.getGroupId() )
                && Objects.equals( this.getArtifactId(), that.getArtifactId() )
                && Objects.equals( this.getVersion(), that.getVersion() )
                && Objects.equals( this.getClassifier(), that.getClassifier() )
                && Objects.equals( this.getType(), that.getType() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( getGroupId(), getArtifactId(), getVersion(), getClassifier(), getType() );
    }

    @Override
    public String toString()
    {
        return coordinate.toString();
    }
}
