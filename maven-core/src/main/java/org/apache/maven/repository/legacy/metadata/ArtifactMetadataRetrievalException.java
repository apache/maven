/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.repository.legacy.metadata;

import org.apache.maven.artifact.Artifact;

/**
 * Error while retrieving repository metadata from the repository.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 */
public class ArtifactMetadataRetrievalException extends Exception {
    private Artifact artifact;

    /** @deprecated use {@link #ArtifactMetadataRetrievalException(String, Throwable, Artifact)} */
    @Deprecated
    public ArtifactMetadataRetrievalException(String message) {
        this(message, null, null);
    }

    /** @deprecated use {@link #ArtifactMetadataRetrievalException(String, Throwable, Artifact)} */
    @Deprecated
    public ArtifactMetadataRetrievalException(Throwable cause) {
        this(null, cause, null);
    }

    /** @deprecated use {@link #ArtifactMetadataRetrievalException(String, Throwable, Artifact)} */
    @Deprecated
    public ArtifactMetadataRetrievalException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public ArtifactMetadataRetrievalException(String message, Throwable cause, Artifact artifact) {
        super(message, cause);
        this.artifact = artifact;
    }

    public Artifact getArtifact() {
        return artifact;
    }
}
