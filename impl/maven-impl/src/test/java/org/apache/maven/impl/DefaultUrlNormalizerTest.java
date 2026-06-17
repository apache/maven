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

    private final DefaultUrlNormalizer sut = new DefaultUrlNormalizer();

    @Test
    void normalizeShouldHandleNullAndEdgeCases() {
        assertNull(sut.normalize(null));
        assertEquals("", sut.normalize(""));
        assertEquals("/", sut.normalize("/../"));
        assertEquals("", sut.normalize("a/../"));
        assertEquals("b", sut.normalize("a/../b"));
        assertEquals("b/d", sut.normalize("a/../b/c/../d"));
        assertEquals("b/c/d", sut.normalize("a/../b/c/d"));
        assertEquals("b/c", sut.normalize("a/../b/c"));
        assertEquals("b/", sut.normalize("a/../b/c/../"));
        assertEquals("../", sut.normalize("../"));
    }

    @Test
    void normalizeShouldPreserveHttpUrlTrailingSlash() {
        assertEquals("https://example.com/path", sut.normalize("https://example.com/path"));
        assertEquals("https://example.com/path/", sut.normalize("https://example.com/path/"));
    }

    @Test
    void normalizeShouldCollapseParentReferencesInUrl() {
        assertEquals("https://example.com/child", sut.normalize("https://example.com/parent/../child"));
        assertEquals("https://example.com/child", sut.normalize("https://example.com/grand/parent/../../child"));
    }

    @Test
    void normalizeHandlesDoubleSlashesAfterParent() {
        assertEquals("https://example.com//child", sut.normalize("https://example.com/parent/..//child"));
        assertEquals("https://example.com/child", sut.normalize("https://example.com/parent//../child"));
    }

    @Test
    void normalizeShouldPreserveOriginalUrlStructure() {
        assertEquals("file:////some/server", sut.normalize("file:////some/server"));
        assertEquals("https://example.com/a%20b/c%20d", sut.normalize("https://example.com/a%20b/c%20d"));
        assertEquals("https://example.com/a b/c d", sut.normalize("https://example.com/a b/c d"));
        assertEquals("ht!tps:/bad_url", sut.normalize("ht!tps:/bad_url"));
    }
}
