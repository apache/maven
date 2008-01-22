package org.apache.maven.plugin.version;

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

import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;

public class PluginVersionResolutionException
    extends Exception
{
    private final String groupId;

    private final String artifactId;

    private final String baseMessage;

    public PluginVersionResolutionException( String groupId, String artifactId, String baseMessage, ArtifactMetadataRetrievalException cause )
    {
        super( "Error resolving version for \'" + groupId + ":" + artifactId + "\': " + baseMessage, cause );

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.baseMessage = baseMessage;
    }

    public PluginVersionResolutionException( String groupId, String artifactId, String baseMessage, InvalidVersionSpecificationException cause )
    {
        super( "Error resolving version for \'" + groupId + ":" + artifactId + "\': " + baseMessage, cause );

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.baseMessage = baseMessage;
    }

    public PluginVersionResolutionException( String groupId, String artifactId, String baseMessage )
    {
        super( "Error resolving version for \'" + groupId + ":" + artifactId + "\': " + baseMessage );

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.baseMessage = baseMessage;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getBaseMessage()
    {
        return baseMessage;
    }

}
