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
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;

public class ProjectArtifactFactory
    extends DefaultArtifactFactory
{
    
    public Artifact create( MavenProject project )
    {
        ArtifactHandler handler = getArtifactHandlerManager().getArtifactHandler( project.getPackaging() );

        return new DefaultArtifact( project.getGroupId(), project.getArtifactId(),
                                    VersionRange.createFromVersion( project.getVersion() ), null,
                                    project.getPackaging(), null, handler, false );
    }

    public Artifact create( MavenProject project, String type, String classifier, boolean optional )
    {
        ArtifactHandler handler = getArtifactHandlerManager().getArtifactHandler( type );

        return new DefaultArtifact( project.getGroupId(), project.getArtifactId(),
                                    VersionRange.createFromVersion( project.getVersion() ), null,
                                    project.getPackaging(), null, handler, optional );
    }

}
