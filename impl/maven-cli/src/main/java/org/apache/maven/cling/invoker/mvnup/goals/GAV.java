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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.util.Objects;

/**
 * Represents a Maven GAV (GroupId, ArtifactId, Version) coordinate.
 *
 * @param groupId the Maven groupId
 * @param artifactId the Maven artifactId
 * @param version the Maven version
 */
public record GAV(String groupId, String artifactId, String version) {

    /**
     * Checks if this GAV matches another GAV ignoring the version.
     *
     * @param other the other GAV to compare
     * @return true if groupId and artifactId match
     */
    public boolean matchesIgnoringVersion(GAV other) {
        if (other == null) {
            return false;
        }
        return Objects.equals(this.groupId, other.groupId) && Objects.equals(this.artifactId, other.artifactId);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
