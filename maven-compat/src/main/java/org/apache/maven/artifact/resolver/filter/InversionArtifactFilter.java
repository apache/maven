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
package org.apache.maven.artifact.resolver.filter;

import org.apache.maven.artifact.Artifact;

/**
 * InversionArtifactFilter
 */
public class InversionArtifactFilter implements ArtifactFilter {
    private final ArtifactFilter toInvert;

    public InversionArtifactFilter(ArtifactFilter toInvert) {
        this.toInvert = toInvert;
    }

    public boolean include(Artifact artifact) {
        return !toInvert.include(artifact);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + toInvert.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof InversionArtifactFilter)) {
            return false;
        }

        InversionArtifactFilter other = (InversionArtifactFilter) obj;

        return toInvert.equals(other.toInvert);
    }
}
