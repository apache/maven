package org.apache.maven.repository.internal;

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

import java.io.File;
import java.util.Map;
import java.util.Objects;

import org.eclipse.aether.artifact.AbstractArtifact;
import org.eclipse.aether.artifact.Artifact;

/**
 * @author Benjamin Bentmann
 */
final class RelocatedArtifact
    extends AbstractArtifact
{

    private final Artifact artifact;

    private final String groupId;

    private final String artifactId;

    private final String version;

    RelocatedArtifact( Artifact artifact, String groupId, String artifactId, String version )
    {
        this.artifact = Objects.requireNonNull( artifact, "artifact cannot be null" );
        // TODO Use StringUtils here
        this.groupId = ( groupId != null && groupId.length() > 0 ) ? groupId : null;
        this.artifactId = ( artifactId != null && artifactId.length() > 0 ) ? artifactId : null;
        this.version = ( version != null && version.length() > 0 ) ? version : null;
    }

    public String getGroupId()
    {
        if ( groupId != null )
        {
            return groupId;
        }
        else
        {
            return artifact.getGroupId();
        }
    }

    public String getArtifactId()
    {
        if ( artifactId != null )
        {
            return artifactId;
        }
        else
        {
            return artifact.getArtifactId();
        }
    }

    public String getVersion()
    {
        if ( version != null )
        {
            return version;
        }
        else
        {
            return artifact.getVersion();
        }
    }

    public String getClassifier()
    {
        return artifact.getClassifier();
    }

    public String getExtension()
    {
        return artifact.getExtension();
    }

    public File getFile()
    {
        return artifact.getFile();
    }

    public String getProperty( String key, String defaultValue )
    {
        return artifact.getProperty( key, defaultValue );
    }

    public Map<String, String> getProperties()
    {
        return artifact.getProperties();
    }

}
