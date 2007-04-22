package org.apache.maven.artifact.repository.metadata;

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

import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Describes repository directory metadata.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo not happy about the store method - they use "this"
 */
public interface RepositoryMetadata
    extends ArtifactMetadata
{
    /**
     * Set the repository the metadata was located in.
     *
     * @param remoteRepository the repository
     */
    void setRepository( ArtifactRepository remoteRepository );

    /**
     * Get the repository metadata associated with this marker.
     *
     * @return the metadata, or <code>null</code> if none loaded
     */
    Metadata getMetadata();

    /**
     * Set the metadata contents.
     *
     * @param metadata the metadata
     */
    void setMetadata( Metadata metadata );

    /**
     * Whether this represents a snapshot.
     *
     * @return if it is a snapshot
     */
    boolean isSnapshot();
}
