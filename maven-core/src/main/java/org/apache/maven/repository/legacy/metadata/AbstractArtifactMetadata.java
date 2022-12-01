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
 * Common elements of artifact metadata.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractArtifactMetadata implements ArtifactMetadata {
    protected Artifact artifact;

    protected AbstractArtifactMetadata(Artifact artifact) {
        this.artifact = artifact;
    }

    public boolean storedInGroupDirectory() {
        return false;
    }

    public String getGroupId() {
        return artifact.getGroupId();
    }

    public String getArtifactId() {
        return artifact.getArtifactId();
    }

    public String extendedToString() {
        StringBuilder buffer = new StringBuilder(256);

        buffer.append("\nArtifact Metadata\n--------------------------");
        buffer.append("\nGroupId: ").append(getGroupId());
        buffer.append("\nArtifactId: ").append(getArtifactId());
        buffer.append("\nMetadata Type: ").append(getClass().getName());

        return buffer.toString();
    }
}
