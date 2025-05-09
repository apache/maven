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
package org.apache.maven.artifact.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Deprecated
class MavenArtifactRepositoryTest {
    private static class MavenArtifactRepositorySubclass extends MavenArtifactRepository {
        String id;

        MavenArtifactRepositorySubclass(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }
    }

    @Test
    void hashCodeEquals() {
        MavenArtifactRepositorySubclass r1 = new MavenArtifactRepositorySubclass("foo");
        MavenArtifactRepositorySubclass r2 = new MavenArtifactRepositorySubclass("foo");
        MavenArtifactRepositorySubclass r3 = new MavenArtifactRepositorySubclass("bar");

        assertThat(r2.hashCode()).isEqualTo(r1.hashCode());
        assertThat(r1.hashCode() == r3.hashCode()).isFalse();

        assertThat(r2).isEqualTo(r1);
        assertThat(r1).isEqualTo(r2);

        assertThat(r3).isNotEqualTo(r1);
        assertThat(r1).isNotEqualTo(r3);
    }
}
