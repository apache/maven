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

public interface ArtifactFactory
{
    static String ROLE = ArtifactFactory.class.getName();

    Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type,
                             String inheritedScope );

    Artifact createArtifact( String groupId, String artifactId, String knownVersion, String scope, String type );
    
    Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String scope,
                                                  String type, String classifier );
}
