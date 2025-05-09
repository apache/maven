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
package org.apache.maven.repository;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.repository.legacy.repository.ArtifactRepositoryFactory;
import org.apache.maven.settings.Mirror;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@PlexusTest
@Deprecated
class MirrorProcessorTest {
    @Inject
    private DefaultMirrorSelector mirrorSelector;

    @Inject
    private ArtifactRepositoryFactory repositorySystem;

    @Test
    void externalURL() {
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://somehost"))).isTrue();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://somehost:9090/somepath"))).isTrue();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "ftp://somehost"))).isTrue();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://192.168.101.1"))).isTrue();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://"))).isTrue();
        // these are local
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://localhost:8080"))).isFalse();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://127.0.0.1:9090"))).isFalse();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "file://localhost/somepath"))).isFalse();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "file://localhost/D:/somepath"))).isFalse();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://localhost"))).isFalse();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://127.0.0.1"))).isFalse();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "file:///somepath"))).isFalse();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "file://D:/somepath"))).isFalse();

        // not a proper url so returns false;
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "192.168.101.1"))).isFalse();
        assertThat(DefaultMirrorSelector.isExternalRepo(getRepo("foo", ""))).isFalse();
    }

    @Test
    void mirrorLookup() {
        Mirror mirrorA = newMirror("a", "a", "http://a");
        Mirror mirrorB = newMirror("b", "b", "http://b");

        List<Mirror> mirrors = Arrays.asList(mirrorA, mirrorB);

        assertThat(mirrorSelector.getMirror(getRepo("a", "http://a.a"), mirrors)).isSameAs(mirrorA);

        assertThat(mirrorSelector.getMirror(getRepo("b", "http://a.a"), mirrors)).isSameAs(mirrorB);

        assertThat(mirrorSelector.getMirror(getRepo("c", "http://c.c"), mirrors)).isNull();
    }

    @Test
    void mirrorWildcardLookup() {
        Mirror mirrorA = newMirror("a", "a", "http://a");
        Mirror mirrorB = newMirror("b", "b", "http://b");
        Mirror mirrorC = newMirror("c", "*", "http://wildcard");

        List<Mirror> mirrors = Arrays.asList(mirrorA, mirrorB, mirrorC);

        assertThat(mirrorSelector.getMirror(getRepo("a", "http://a.a"), mirrors)).isSameAs(mirrorA);

        assertThat(mirrorSelector.getMirror(getRepo("b", "http://a.a"), mirrors)).isSameAs(mirrorB);

        assertThat(mirrorSelector.getMirror(getRepo("c", "http://c.c"), mirrors)).isSameAs(mirrorC);
    }

    @Test
    void mirrorStopOnFirstMatch() {
        // exact matches win first
        Mirror mirrorA2 = newMirror("a2", "a,b", "http://a2");
        Mirror mirrorA = newMirror("a", "a", "http://a");
        // make sure repeated entries are skipped
        Mirror mirrorA3 = newMirror("a", "a", "http://a3");

        Mirror mirrorB = newMirror("b", "b", "http://b");
        Mirror mirrorC = newMirror("c", "d,e", "http://de");
        Mirror mirrorC2 = newMirror("c", "*", "http://wildcard");
        Mirror mirrorC3 = newMirror("c", "e,f", "http://ef");

        List<Mirror> mirrors = Arrays.asList(mirrorA2, mirrorA, mirrorA3, mirrorB, mirrorC, mirrorC2, mirrorC3);

        assertThat(mirrorSelector.getMirror(getRepo("a", "http://a.a"), mirrors)).isSameAs(mirrorA);

        assertThat(mirrorSelector.getMirror(getRepo("b", "http://a.a"), mirrors)).isSameAs(mirrorB);

        assertThat(mirrorSelector.getMirror(getRepo("c", "http://c.c"), mirrors)).isSameAs(mirrorC2);

        assertThat(mirrorSelector.getMirror(getRepo("d", "http://d"), mirrors)).isSameAs(mirrorC);

        assertThat(mirrorSelector.getMirror(getRepo("e", "http://e"), mirrors)).isSameAs(mirrorC);

        assertThat(mirrorSelector.getMirror(getRepo("f", "http://f"), mirrors)).isSameAs(mirrorC2);
    }

    @Test
    void patterns() {
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "*")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "*,")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), ",*,")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "*,")).isTrue();

        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "a")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "a,")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), ",a,")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "a,")).isTrue();

        assertThat(DefaultMirrorSelector.matchPattern(getRepo("b"), "a")).isFalse();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("b"), "a,")).isFalse();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("b"), ",a")).isFalse();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("b"), ",a,")).isFalse();

        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "a,b")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("b"), "a,b")).isTrue();

        assertThat(DefaultMirrorSelector.matchPattern(getRepo("c"), "a,b")).isFalse();

        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "*")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "*,b")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "*,!b")).isTrue();

        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "*,!a")).isFalse();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a"), "!a,*")).isFalse();

        assertThat(DefaultMirrorSelector.matchPattern(getRepo("c"), "*,!a")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("c"), "!a,*")).isTrue();

        assertThat(DefaultMirrorSelector.matchPattern(getRepo("c"), "!a,!c")).isFalse();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("d"), "!a,!c*")).isFalse();
    }

    @Test
    void patternsWithExternal() {
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "*")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "external:*")).isFalse();

        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "external:*,a")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "external:*,!a")).isFalse();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "a,external:*")).isTrue();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "!a,external:*")).isFalse();

        assertThat(DefaultMirrorSelector.matchPattern(getRepo("c", "http://localhost"), "!a,external:*")).isFalse();
        assertThat(DefaultMirrorSelector.matchPattern(getRepo("c", "http://somehost"), "!a,external:*")).isTrue();
    }

    @Test
    void layoutPattern() {
        assertThat(DefaultMirrorSelector.matchesLayout("default", null)).isTrue();
        assertThat(DefaultMirrorSelector.matchesLayout("default", "")).isTrue();
        assertThat(DefaultMirrorSelector.matchesLayout("default", "*")).isTrue();

        assertThat(DefaultMirrorSelector.matchesLayout("default", "default")).isTrue();
        assertThat(DefaultMirrorSelector.matchesLayout("default", "legacy")).isFalse();

        assertThat(DefaultMirrorSelector.matchesLayout("default", "legacy,default")).isTrue();
        assertThat(DefaultMirrorSelector.matchesLayout("default", "default,legacy")).isTrue();

        assertThat(DefaultMirrorSelector.matchesLayout("default", "legacy,!default")).isFalse();
        assertThat(DefaultMirrorSelector.matchesLayout("default", "!default,legacy")).isFalse();

        assertThat(DefaultMirrorSelector.matchesLayout("default", "*,!default")).isFalse();
        assertThat(DefaultMirrorSelector.matchesLayout("default", "!default,*")).isFalse();
    }

    @Test
    void mirrorLayoutConsideredForMatching() {
        ArtifactRepository repo = getRepo("a");

        Mirror mirrorA = newMirror("a", "a", null, "http://a");
        Mirror mirrorB = newMirror("b", "a", "p2", "http://b");

        Mirror mirrorC = newMirror("c", "*", null, "http://c");
        Mirror mirrorD = newMirror("d", "*", "p2", "http://d");

        assertThat(mirrorSelector.getMirror(repo, Arrays.asList(mirrorA))).isSameAs(mirrorA);
        assertThat(mirrorSelector.getMirror(repo, Arrays.asList(mirrorB))).isNull();

        assertThat(mirrorSelector.getMirror(repo, Arrays.asList(mirrorC))).isSameAs(mirrorC);
        assertThat(mirrorSelector.getMirror(repo, Arrays.asList(mirrorD))).isNull();
    }

    /**
     * Build an ArtifactRepository object.
     *
     * @param id
     * @param url
     * @return
     */
    private ArtifactRepository getRepo(String id, String url) {
        return repositorySystem.createArtifactRepository(id, url, new DefaultRepositoryLayout(), null, null);
    }

    /**
     * Build an ArtifactRepository object.
     *
     * @param id
     * @return
     */
    private ArtifactRepository getRepo(String id) {
        return getRepo(id, "http://something");
    }

    private Mirror newMirror(String id, String mirrorOf, String url) {
        return newMirror(id, mirrorOf, null, url);
    }

    private Mirror newMirror(String id, String mirrorOf, String layouts, String url) {
        Mirror mirror = new Mirror();

        mirror.setId(id);
        mirror.setMirrorOf(mirrorOf);
        mirror.setMirrorOfLayouts(layouts);
        mirror.setUrl(url);

        return mirror;
    }
}
