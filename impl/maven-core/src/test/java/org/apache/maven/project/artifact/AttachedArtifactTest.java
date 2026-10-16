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
package org.apache.maven.project.artifact;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class AttachedArtifactTest {

    private final ArtifactHandler handler = new DefaultArtifactHandler("jar");

    @Test
    void testVersionIsTheOneOfTheParent() {
        Artifact parent = new DefaultArtifact("g", "a", "1.0", "compile", "jar", null, handler);

        Artifact attached = new AttachedArtifact(parent, "jar", "sources", handler);

        assertEquals("1.0", attached.getVersion());
    }

    /**
     * A parent which has a version but no version range, as after {@link Artifact#setVersion(String)}, is not a
     * valid base for an attached artifact. This is reported as such, not as a NullPointerException raised while
     * the error message is built.
     */
    @Test
    void testParentWithoutVersionRangeIsRejected() {
        Artifact parent = new DefaultArtifact("g", "a", "1.0", "compile", "jar", null, handler);
        parent.setVersion("1.0");

        InvalidArtifactRTException e = assertThrows(
                InvalidArtifactRTException.class, () -> new AttachedArtifact(parent, "jar", "sources", handler));

        assertTrue(e.getMessage().contains("The version cannot be empty."), e.getMessage());
    }
}
