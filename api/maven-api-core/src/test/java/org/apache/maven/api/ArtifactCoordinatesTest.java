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
package org.apache.maven.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactCoordinatesTest {

    @Test
    void getIdOmitsEmptyClassifier() {
        ArtifactCoordinates coords = stub("org.example", "demo", "jar", "", "1.0");
        assertEquals("org.example:demo:jar:1.0", coords.getId());
    }

    @Test
    void getIdIncludesNonEmptyClassifier() {
        ArtifactCoordinates coords = stub("org.example", "demo", "jar", "sources", "1.0");
        assertEquals("org.example:demo:jar:sources:1.0", coords.getId());
    }

    private static ArtifactCoordinates stub(
            String groupId, String artifactId, String extension, String classifier, String version) {
        return new ArtifactCoordinates() {
            @Override
            public String getGroupId() {
                return groupId;
            }

            @Override
            public String getArtifactId() {
                return artifactId;
            }

            @Override
            public String getClassifier() {
                return classifier;
            }

            @Override
            public VersionConstraint getVersionConstraint() {
                return new VersionConstraint() {
                    @Override
                    public VersionRange getVersionRange() {
                        return null;
                    }

                    @Override
                    public Version getRecommendedVersion() {
                        return null;
                    }

                    @Override
                    public boolean contains(Version v) {
                        return false;
                    }

                    @Override
                    public String toString() {
                        return version;
                    }
                };
            }

            @Override
            public String getExtension() {
                return extension;
            }
        };
    }
}
