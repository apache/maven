package org.apache.maven.model.building;

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
import java.util.Objects;

import org.apache.maven.building.FileSource;

/**
 * Represents a model pulled from a repository
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public class ArtifactModelSource extends FileSource implements ModelSource
{
    private final String groupId;

    private final String artifactId;

    private final String version;

    private final int hashCode;

    public ArtifactModelSource( File file, String groupId, String artifactId, String version )
    {
        super( file );
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.hashCode = Objects.hash( groupId, artifactId, version );
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }

        if ( !ArtifactModelSource.class.equals( obj.getClass() )  )
        {
            return false;
        }

        ArtifactModelSource other = (ArtifactModelSource) obj;
        return Objects.equals( artifactId, other.artifactId )
            && Objects.equals( groupId, other.groupId )
            && Objects.equals( version, other.version );
    }
}
