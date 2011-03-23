package org.apache.maven.artifact.resolver;

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
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * Listens to the resolution process and handles events.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ResolutionListener
{
    String ROLE = ResolutionListener.class.getName();

    int TEST_ARTIFACT = 1;

    int PROCESS_CHILDREN = 2;

    int FINISH_PROCESSING_CHILDREN = 3;

    int INCLUDE_ARTIFACT = 4;

    int OMIT_FOR_NEARER = 5;

    int UPDATE_SCOPE = 6;

    @Deprecated
    int MANAGE_ARTIFACT = 7;

    int OMIT_FOR_CYCLE = 8;

    /**
     * this event means that the artifactScope has NOT been updated to a farther node artifactScope because current
     * node is in the first level pom
     */
    int UPDATE_SCOPE_CURRENT_POM = 9;

    int SELECT_VERSION_FROM_RANGE = 10;

    int RESTRICT_RANGE = 11;

    int MANAGE_ARTIFACT_VERSION = 12;

    int MANAGE_ARTIFACT_SCOPE = 13;

    int MANAGE_ARTIFACT_SYSTEM_PATH = 14;

    void testArtifact( Artifact node );

    void startProcessChildren( Artifact artifact );

    void endProcessChildren( Artifact artifact );

    void includeArtifact( Artifact artifact );

    void omitForNearer( Artifact omitted,
                        Artifact kept );

    void updateScope( Artifact artifact,
                      String scope );

    @Deprecated
    void manageArtifact( Artifact artifact,
                         Artifact replacement );

    // TODO Use the following two instead of manageArtifact
    // TODO Remove ResolutionListenerDM interface

    //void manageArtifactVersion( Artifact artifact, Artifact replacement );

    //void manageArtifactScope( Artifact artifact, Artifact replacement );

    void omitForCycle( Artifact artifact );

    /**
     * This event means that the artifactScope has NOT been updated to a farther node artifactScope because current
     * node is in the first level pom
     *
     * @param artifact     current node artifact, the one in the first level pom
     * @param ignoredScope artifactScope that was ignored because artifact was in first level pom
     */
    void updateScopeCurrentPom( Artifact artifact,
                                String ignoredScope );

    void selectVersionFromRange( Artifact artifact );

    void restrictRange( Artifact artifact,
                        Artifact replacement,
                        VersionRange newRange );
}
