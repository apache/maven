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

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
@Deprecated
class DefaultUrlNormalizerTest {

    private UrlNormalizer normalizer = new DefaultUrlNormalizer();

    private String normalize(String url) {
        return normalizer.normalize(url);
    }

    @Test
    void nullSafe() {
        assertThat(normalize(null)).isNull();
    }

    @Test
    void trailingSlash() {
        assertThat(normalize("")).isEqualTo("");
        assertThat(normalize("http://server.org/dir")).isEqualTo("http://server.org/dir");
        assertThat(normalize("http://server.org/dir/")).isEqualTo("http://server.org/dir/");
    }

    @Test
    void removalOfParentRefs() {
        assertThat(normalize("http://server.org/parent/../child")).isEqualTo("http://server.org/child");
        assertThat(normalize("http://server.org/grand/parent/../../child")).isEqualTo("http://server.org/child");

        assertThat(normalize("http://server.org/parent/..//child")).isEqualTo("http://server.org//child");
        assertThat(normalize("http://server.org/parent//../child")).isEqualTo("http://server.org/child");
    }

    @Test
    void preservationOfDoubleSlashes() {
        assertThat(normalize("scm:hg:ssh://localhost//home/user")).isEqualTo("scm:hg:ssh://localhost//home/user");
        assertThat(normalize("file:////UNC/server")).isEqualTo("file:////UNC/server");
        assertThat(normalize("[fetch=]http://server.org/[push=]ssh://server.org/")).isEqualTo("[fetch=]http://server.org/[push=]ssh://server.org/");
    }

    @Test
    void absolutePathTraversalPastRootIsOmitted() {
        assertThat(normalize("/../")).isEqualTo("/");
    }

    @Test
    void parentDirectoryRemovedFromRelativeUriReference() {
        assertThat(normalize("a/../")).isEqualTo("");
    }

    @Test
    void leadingParentDirectoryNotRemovedFromRelativeUriReference() {
        assertThat(normalize("../")).isEqualTo("../");
    }
}
