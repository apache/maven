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
package org.apache.maven.internal.impl;

import java.util.Objects;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Nonnull;

import static org.apache.maven.internal.impl.Utils.nonNull;

/**
 * A wrapper class around a maven resolver artifact.
 */
public class DefaultArtifact
    implements Artifact
{
    private final @Nonnull AbstractSession session;

    private final @Nonnull org.eclipse.aether.artifact.Artifact artifact;

    private final String id;

    public DefaultArtifact( @Nonnull
    AbstractSession session, @Nonnull
    org.eclipse.aether.artifact.Artifact artifact )
    {
        this.session = nonNull( session, "session can not be null" );
        this.artifact = nonNull( artifact, "artifact can not be null" );
        this.id = getGroupId() + ':' + getArtifactId() + ':' + getExtension()
            + ( getClassifier().length() > 0 ? ":" + getClassifier() : "" ) + ':' + getVersion();
    }

    public org.eclipse.aether.artifact.Artifact getArtifact()
    {
        return artifact;
    }

    @Override
    public String key()
    {
        return id;
    }

    @Nonnull
    @Override
    public String getGroupId()
    {
        return artifact.getGroupId();
    }

    @Nonnull
    @Override
    public String getArtifactId()
    {
        return artifact.getArtifactId();
    }

    @Nonnull
    @Override
    public Version getVersion()
    {
        return session.parseVersion( artifact.getVersion() );
    }

    @Nonnull
    @Override
    public String getExtension()
    {
        return artifact.getExtension();
    }

    @Nonnull
    @Override
    public String getClassifier()
    {
        return artifact.getClassifier();
    }

    @Override
    public boolean isSnapshot()
    {
        return DefaultVersionParser.checkSnapshot( artifact.getVersion() );
    }

    @Nonnull
    @Override
    public ArtifactCoordinate toCoordinate()
    {
        return session.createArtifactCoordinate( this );
    }

    @Override
    public boolean equals( Object o )
    {
        return o instanceof DefaultArtifact && Objects.equals( id, ( (DefaultArtifact) o ).id );
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public String toString()
    {
        return artifact.toString();
    }
}
