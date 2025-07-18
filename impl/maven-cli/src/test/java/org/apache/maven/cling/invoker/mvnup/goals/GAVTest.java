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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link GAV} record class.
 * Tests the Maven GroupId, ArtifactId, Version coordinate functionality.
 */
@DisplayName("GAV")
class GAVTest {

    @Nested
    @DisplayName("Equality")
    class EqualityTests {

        @Test
        @DisplayName("should be equal when all components match")
        void shouldBeEqualWhenAllComponentsMatch() {
            GAV gav1 = new GAV("com.example", "artifact", "1.0.0");
            GAV gav2 = new GAV("com.example", "artifact", "1.0.0");

            assertEquals(gav1, gav2);
            assertEquals(gav1.hashCode(), gav2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when versions differ")
        void shouldNotBeEqualWhenVersionsDiffer() {
            GAV gav1 = new GAV("com.example", "artifact", "1.0.0");
            GAV gav2 = new GAV("com.example", "artifact", "2.0.0");

            assertNotEquals(gav1, gav2);
        }

        @Test
        @DisplayName("should not be equal when groupIds differ")
        void shouldNotBeEqualWhenGroupIdsDiffer() {
            GAV gav1 = new GAV("com.example", "artifact", "1.0.0");
            GAV gav2 = new GAV("org.example", "artifact", "1.0.0");

            assertNotEquals(gav1, gav2);
        }

        @Test
        @DisplayName("should not be equal when artifactIds differ")
        void shouldNotBeEqualWhenArtifactIdsDiffer() {
            GAV gav1 = new GAV("com.example", "artifact1", "1.0.0");
            GAV gav2 = new GAV("com.example", "artifact2", "1.0.0");

            assertNotEquals(gav1, gav2);
        }
    }

    @Nested
    @DisplayName("matchesIgnoringVersion()")
    class MatchesIgnoringVersionTests {

        @Test
        @DisplayName("should match when groupId and artifactId are same but version differs")
        void shouldMatchWhenGroupIdAndArtifactIdSameButVersionDiffers() {
            GAV gav1 = new GAV("com.example", "artifact", "1.0.0");
            GAV gav2 = new GAV("com.example", "artifact", "2.0.0");

            assertTrue(gav1.matchesIgnoringVersion(gav2));
            assertTrue(gav2.matchesIgnoringVersion(gav1));
        }

        @Test
        @DisplayName("should match when all components are identical")
        void shouldMatchWhenAllComponentsIdentical() {
            GAV gav1 = new GAV("com.example", "artifact", "1.0.0");
            GAV gav2 = new GAV("com.example", "artifact", "1.0.0");

            assertTrue(gav1.matchesIgnoringVersion(gav2));
        }

        @Test
        @DisplayName("should not match when groupIds differ")
        void shouldNotMatchWhenGroupIdsDiffer() {
            GAV gav1 = new GAV("com.example", "artifact", "1.0.0");
            GAV gav2 = new GAV("org.example", "artifact", "1.0.0");

            assertFalse(gav1.matchesIgnoringVersion(gav2));
        }

        @Test
        @DisplayName("should not match when artifactIds differ")
        void shouldNotMatchWhenArtifactIdsDiffer() {
            GAV gav1 = new GAV("com.example", "artifact1", "1.0.0");
            GAV gav2 = new GAV("com.example", "artifact2", "1.0.0");

            assertFalse(gav1.matchesIgnoringVersion(gav2));
        }

        @Test
        @DisplayName("should return false when other GAV is null")
        void shouldReturnFalseWhenOtherGAVIsNull() {
            GAV gav = new GAV("com.example", "artifact", "1.0.0");

            assertFalse(gav.matchesIgnoringVersion(null));
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("should format as groupId:artifactId:version")
        void shouldFormatAsGroupIdArtifactIdVersion() {
            GAV gav = new GAV("com.example", "my-artifact", "1.2.3");

            assertEquals("com.example:my-artifact:1.2.3", gav.toString());
        }

        @Test
        @DisplayName("should handle null components gracefully")
        void shouldHandleNullComponentsGracefully() {
            GAV gav = new GAV(null, null, null);

            assertEquals("null:null:null", gav.toString());
        }
    }
}
