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
package org.apache.maven.model.path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 */
class DefaultUrlNormalizerTest {

    private UrlNormalizer normalizer = new DefaultUrlNormalizer();

    private String normalize(String url) {
        return normalizer.normalize(url);
    }

    @Test
    void testNullSafe() {
        assertNull(normalize(null));
    }

    @Test
    void testTrailingSlash() {
        assertEquals("", normalize(""));
        assertEquals("http://server.org/dir", normalize("http://server.org/dir"));
        assertEquals("http://server.org/dir/", normalize("http://server.org/dir/"));
    }

    @Test
    void testRemovalOfParentRefs() {
        assertEquals("http://server.org/child", normalize("http://server.org/parent/../child"));
        assertEquals("http://server.org/child", normalize("http://server.org/grand/parent/../../child"));

        assertEquals("http://server.org//child", normalize("http://server.org/parent/..//child"));
        assertEquals("http://server.org/child", normalize("http://server.org/parent//../child"));
    }

    @Test
    void testPreservationOfDoubleSlashes() {
        assertEquals("scm:hg:ssh://localhost//home/user", normalize("scm:hg:ssh://localhost//home/user"));
        assertEquals("file:////UNC/server", normalize("file:////UNC/server"));
        assertEquals(
                "[fetch=]http://server.org/[push=]ssh://server.org/",
                normalize("[fetch=]http://server.org/[push=]ssh://server.org/"));
    }

    @Test
    void absolutePathTraversalPastRootIsOmitted() {
        assertEquals("/", normalize("/../"));
    }

    @Test
    void parentDirectoryRemovedFromRelativeUriReference() {
        assertEquals("", normalize("a/../"));
    }

    @Test
    void leadingParentDirectoryNotRemovedFromRelativeUriReference() {
        assertEquals("../", normalize("../"));
    }
}
