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
package org.apache.maven.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefaultUrlNormalizerTest {

    @Test
    void normalizeShouldHandleNullAndEdgeCases() {
        assertNull(DefaultUrlNormalizer.normalize(null));
        assertEquals("", DefaultUrlNormalizer.normalize(""));
        assertEquals("/", DefaultUrlNormalizer.normalize("/../"));
        assertEquals("", DefaultUrlNormalizer.normalize("a/../"));
        assertEquals("b", DefaultUrlNormalizer.normalize("a/../b"));
        assertEquals("b/d", DefaultUrlNormalizer.normalize("a/../b/c/../d"));
        assertEquals("b/c/d", DefaultUrlNormalizer.normalize("a/../b/c/d"));
        assertEquals("b/c", DefaultUrlNormalizer.normalize("a/../b/c"));
        assertEquals("b/", DefaultUrlNormalizer.normalize("a/../b/c/../"));
        assertEquals("../", DefaultUrlNormalizer.normalize("../"));
    }

    @Test
    void normalizeShouldPreserveHttpUrlTrailingSlash() {
        assertEquals("https://example.com/path", DefaultUrlNormalizer.normalize("https://example.com/path"));
        assertEquals("https://example.com/path/", DefaultUrlNormalizer.normalize("https://example.com/path/"));
    }

    @Test
    void normalizeShouldCollapseParentReferencesInUrl() {
        assertEquals(
                "https://example.com/child", DefaultUrlNormalizer.normalize("https://example.com/parent/../child"));
        assertEquals(
                "https://example.com/child",
                DefaultUrlNormalizer.normalize("https://example.com/grand/parent/../../child"));
    }

    @Test
    void normalizeHandlesDoubleSlashesAfterParent() {
        assertEquals(
                "https://example.com//child", DefaultUrlNormalizer.normalize("https://example.com/parent/..//child"));
        assertEquals(
                "https://example.com/child", DefaultUrlNormalizer.normalize("https://example.com/parent//../child"));
    }

    @Test
    void normalizeShouldPreserveOriginalUrlStructure() {
        assertEquals("file:////some/server", DefaultUrlNormalizer.normalize("file:////some/server"));
        assertEquals(
                "https://example.com/a%20b/c%20d", DefaultUrlNormalizer.normalize("https://example.com/a%20b/c%20d"));
        assertEquals("https://example.com/a b/c d", DefaultUrlNormalizer.normalize("https://example.com/a b/c d"));
        assertEquals("ht!tps:/bad_url", DefaultUrlNormalizer.normalize("ht!tps:/bad_url"));
    }
}
