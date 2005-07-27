package org.apache.maven.artifact.factory;

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
import org.apache.maven.artifact.versioning.VersionRange;

public interface ArtifactFactory
{
    String ROLE = ArtifactFactory.class.getName();

    /**
     * @deprecated
     */
    Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type,
                             String inheritedScope );

    // TODO: deprecate and chase down (probably used for copying only)
    Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type );

    /**
     * @deprecated
     */
    Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String scope, String type,
                                           String classifier );

    Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type,
                                           String classifier );

    Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type,
                                       String scope );

    Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type,
                                       String scope, String inheritedScope );

    Artifact createBuildArtifact( String groupId, String artifactId, String version, String packaging );

    Artifact createProjectArtifact( String groupId, String artifactId, String version );

    Artifact createParentArtifact( String groupId, String artifactId, String version );

    Artifact createPluginArtifact( String groupId, String artifactId, VersionRange versionRange );

    Artifact createProjectArtifact( String groupId, String artifactId, String version, String scope );

    Artifact createExtensionArtifact( String groupId, String artifactId, VersionRange versionRange );
}
