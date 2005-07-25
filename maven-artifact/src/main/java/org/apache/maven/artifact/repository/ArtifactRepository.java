package org.apache.maven.artifact.repository;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.metadata.ArtifactMetadata;

/**
 * Specifies the repository used for artifact handling.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public interface ArtifactRepository
{

    String pathOf( Artifact artifact );

    String pathOfMetadata( ArtifactMetadata artifactMetadata );
    
    String formatAsDirectory( String directory );

    String formatAsFile( String file );

    String getUrl();

    String getBasedir();

    String getProtocol();

    String getId();

    ArtifactRepositoryPolicy getSnapshots();

    ArtifactRepositoryPolicy getReleases();

    ArtifactRepositoryLayout getLayout();
}
