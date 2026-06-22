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
package org.apache.maven.repository.metadata;

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for {@link ArtifactMetadata#isError()}.
 * Verifies that isError() returns true when error is set, false when error is null.
 */
@Deprecated
class ArtifactMetadataTest {

    @Test
    void isErrorReturnsFalseWhenErrorIsNull() {
        ArtifactMetadata metadata = new ArtifactMetadata("g:a:1.0");
        assertFalse(metadata.isError());
    }

    @Test
    void isErrorReturnsTrueWhenErrorIsSet() {
        ArtifactMetadata metadata = new ArtifactMetadata("g:a:1.0");
        metadata.setError("Something went wrong");
        assertTrue(metadata.isError());
    }

    @Test
    void isErrorReturnsFalseAfterErrorIsCleared() {
        ArtifactMetadata metadata = new ArtifactMetadata("g:a:1.0");
        metadata.setError("Something went wrong");
        metadata.setError(null);
        assertFalse(metadata.isError());
    }

    @Test
    void isErrorReturnsTrueWhenConstructedWithError() {
        ArtifactMetadata metadata = new ArtifactMetadata(
                "g", "a", "1.0", "jar", ArtifactScopeEnum.DEFAULT_SCOPE, null, null, null, false, "Resolution failed");
        assertTrue(metadata.isError());
    }

    @Test
    void isErrorReturnsFalseWhenConstructedWithoutError() {
        ArtifactMetadata metadata = new ArtifactMetadata(
                "g", "a", "1.0", "jar", ArtifactScopeEnum.DEFAULT_SCOPE, null, null, null, true, null);
        assertFalse(metadata.isError());
    }
}
