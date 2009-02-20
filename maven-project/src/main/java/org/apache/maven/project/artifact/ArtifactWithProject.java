package org.apache.maven.project.artifact;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;

import java.util.Collection;
import java.util.Iterator;

public class ArtifactWithProject
    extends DefaultArtifact
{

    private final MavenProject project;

    public ArtifactWithProject( MavenProject project, String type, String classifier,
                                ArtifactHandler artifactHandler, boolean optional )
    {
        super( project.getGroupId(), project.getArtifactId(), VersionRange.createFromVersion( project.getVersion() ),
               null, type, classifier, artifactHandler, optional );
        
        this.project = project;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public ProjectArtifactMetadata getProjectArtifactMetadata()
    {
        return getProjectArtifactMetadata( this );
    }

    public static ProjectArtifactMetadata getProjectArtifactMetadata( Artifact artifact )
    {
        Collection metadataList = artifact.getMetadataList();
        if ( metadataList != null )
        {
            for ( Iterator it = metadataList.iterator(); it.hasNext(); )
            {
                Object metadata = it.next();
                if ( metadata instanceof ProjectArtifactMetadata )
                {
                    return (ProjectArtifactMetadata) metadata;
                }
            }
        }

        return null;
    }

}
