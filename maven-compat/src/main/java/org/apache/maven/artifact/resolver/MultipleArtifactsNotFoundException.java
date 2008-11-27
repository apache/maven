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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Exception caused when one or more artifacts can not be resolved because they are not found in the
 * local or remote repositories.
 */
public class MultipleArtifactsNotFoundException
    extends ArtifactResolutionException
{
    private final List<Artifact> resolvedArtifacts;
    private final List<Artifact> missingArtifacts;

    /** @deprecated use {@link #MultipleArtifactsNotFoundException(Artifact, List, List, List)} */
    @Deprecated
    public MultipleArtifactsNotFoundException( Artifact originatingArtifact,
                                               List<Artifact> missingArtifacts,
                                               List<ArtifactRepository> remoteRepositories )
    {
        this( originatingArtifact, new ArrayList<Artifact>(), missingArtifacts, remoteRepositories );
    }

    /**
     * Create an instance of the exception with allrequired information.
     *
     * @param originatingArtifact the artifact that was being resolved
     * @param resolvedArtifacts   artifacts that could be resolved
     * @param missingArtifacts    artifacts that could not be resolved
     * @param remoteRepositories  remote repositories where the missing artifacts were not found
     */
    public MultipleArtifactsNotFoundException( Artifact originatingArtifact,
                                               List<Artifact> resolvedArtifacts,
                                               List<Artifact> missingArtifacts,
                                               List<ArtifactRepository> remoteRepositories )
    {
        super( constructMessage( missingArtifacts ), originatingArtifact, remoteRepositories );
        this.resolvedArtifacts = resolvedArtifacts;
        this.missingArtifacts = missingArtifacts;
    }

    /**
     * artifacts that could be resolved
     *
     * @return {@link List} of {@link Artifact}
     */
    public List<Artifact> getResolvedArtifacts()
    {
        return resolvedArtifacts;
    }

    /**
     * artifacts that could NOT be resolved
     *
     * @return {@link List} of {@link Artifact}
     */
    public List<Artifact> getMissingArtifacts()
    {
        return missingArtifacts;
    }

    private static String constructMessage( List<Artifact> artifacts )
    {
        StringBuffer buffer = new StringBuffer( "Missing:\n" );

        buffer.append( "----------\n" );

        int counter = 0;

        for (Artifact artifact : artifacts) {
            String message = (++counter) + ") " + artifact.getId();

            buffer.append(constructMissingArtifactMessage(message, "  ", artifact.getGroupId(), artifact
                    .getArtifactId(), artifact.getVersion(), artifact.getType(), artifact.getClassifier(),
                    artifact.getDownloadUrl(), artifact
                    .getDependencyTrail()));
        }

        buffer.append( "----------\n" );

        int size = artifacts.size();

        buffer.append( size ).append( " required artifact" );

        if ( size > 1 )
        {
            buffer.append( "s are" );
        }
        else
        {
            buffer.append( " is" );
        }

        buffer.append( " missing.\n\nfor artifact: " );

        return buffer.toString();
    }

}
