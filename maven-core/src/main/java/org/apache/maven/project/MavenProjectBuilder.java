package org.apache.maven.project;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;

import java.io.File;

public interface MavenProjectBuilder
{
    String ROLE = MavenProjectBuilder.class.getName();
    
    static final String STANDALONE_SUPERPOM_GROUPID = "org.apache.maven";
    
    static final String STANDALONE_SUPERPOM_ARTIFACTID = "super-pom";
    
    static final String STANDALONE_SUPERPOM_VERSION = "2.0";

    MavenProject build( File project, ArtifactRepository localRepository )
        throws ProjectBuildingException;

    MavenProject buildWithDependencies( File project, ArtifactRepository localRepository )
        throws ProjectBuildingException;

    MavenProject buildFromRepository( Artifact artifact, ArtifactRepository localRepository )
        throws ProjectBuildingException;

    MavenProject buildStandaloneSuperProject( ArtifactRepository localRepository )
        throws ProjectBuildingException;

    Model getCachedModel( String groupId, String artifactId, String version );
}
