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

import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.repository.legacy.repository.ArtifactRepositoryFactory;
import org.apache.maven.settings.Mirror;
import org.codehaus.plexus.PlexusTestCase;

public class MirrorProcessorTest extends PlexusTestCase {
    private DefaultMirrorSelector mirrorSelector;
    private ArtifactRepositoryFactory repositorySystem;

    protected void setUp() throws Exception {
        mirrorSelector = (DefaultMirrorSelector) lookup(MirrorSelector.class);
        repositorySystem = lookup(ArtifactRepositoryFactory.class);
    }

    @Override
    protected void tearDown() throws Exception {
        mirrorSelector = null;
        repositorySystem = null;

        super.tearDown();
    }

    public void testExternalURL() {
        assertTrue(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://somehost")));
        assertTrue(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://somehost:9090/somepath")));
        assertTrue(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "ftp://somehost")));
        assertTrue(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://192.168.101.1")));
        assertTrue(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://")));
        // these are local
        assertFalse(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://localhost:8080")));
        assertFalse(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://127.0.0.1:9090")));
        assertFalse(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "file://localhost/somepath")));
        assertFalse(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "file://localhost/D:/somepath")));
        assertFalse(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://localhost")));
        assertFalse(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "http://127.0.0.1")));
        assertFalse(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "file:///somepath")));
        assertFalse(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "file://D:/somepath")));

        // not a proper url so returns false;
        assertFalse(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "192.168.101.1")));
        assertFalse(DefaultMirrorSelector.isExternalRepo(getRepo("foo", "")));
    }

    public void testMirrorLookup() {
        Mirror mirrorA = newMirror("a", "a", "http://a");
        Mirror mirrorB = newMirror("b", "b", "http://b");

        List<Mirror> mirrors = Arrays.asList(mirrorA, mirrorB);

        assertSame(mirrorA, mirrorSelector.getMirror(getRepo("a", "http://a.a"), mirrors));

        assertSame(mirrorB, mirrorSelector.getMirror(getRepo("b", "http://a.a"), mirrors));

        assertNull(mirrorSelector.getMirror(getRepo("c", "http://c.c"), mirrors));
    }

    public void testMirrorWildcardLookup() {
        Mirror mirrorA = newMirror("a", "a", "http://a");
        Mirror mirrorB = newMirror("b", "b", "http://b");
        Mirror mirrorC = newMirror("c", "*", "http://wildcard");

        List<Mirror> mirrors = Arrays.asList(mirrorA, mirrorB, mirrorC);

        assertSame(mirrorA, mirrorSelector.getMirror(getRepo("a", "http://a.a"), mirrors));

        assertSame(mirrorB, mirrorSelector.getMirror(getRepo("b", "http://a.a"), mirrors));

        assertSame(mirrorC, mirrorSelector.getMirror(getRepo("c", "http://c.c"), mirrors));
    }

    public void testMirrorStopOnFirstMatch() {
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

        assertSame(mirrorA, mirrorSelector.getMirror(getRepo("a", "http://a.a"), mirrors));

        assertSame(mirrorB, mirrorSelector.getMirror(getRepo("b", "http://a.a"), mirrors));

        assertSame(mirrorC2, mirrorSelector.getMirror(getRepo("c", "http://c.c"), mirrors));

        assertSame(mirrorC, mirrorSelector.getMirror(getRepo("d", "http://d"), mirrors));

        assertSame(mirrorC, mirrorSelector.getMirror(getRepo("e", "http://e"), mirrors));

        assertSame(mirrorC2, mirrorSelector.getMirror(getRepo("f", "http://f"), mirrors));
    }

    public void testPatterns() {
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), "*"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), "*,"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), ",*,"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), "*,"));

        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), "a"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), "a,"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), ",a,"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), "a,"));

        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("b"), "a"));
        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("b"), "a,"));
        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("b"), ",a"));
        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("b"), ",a,"));

        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), "a,b"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("b"), "a,b"));

        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("c"), "a,b"));

        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), "*"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), "*,b"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a"), "*,!b"));

        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("a"), "*,!a"));
        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("a"), "!a,*"));

        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("c"), "*,!a"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("c"), "!a,*"));

        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("c"), "!a,!c"));
        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("d"), "!a,!c*"));
    }

    public void testPatternsWithExternal() {
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "*"));
        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "external:*"));

        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "external:*,a"));
        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "external:*,!a"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "a,external:*"));
        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("a", "http://localhost"), "!a,external:*"));

        assertFalse(DefaultMirrorSelector.matchPattern(getRepo("c", "http://localhost"), "!a,external:*"));
        assertTrue(DefaultMirrorSelector.matchPattern(getRepo("c", "http://somehost"), "!a,external:*"));
    }

    public void testLayoutPattern() {
        assertTrue(DefaultMirrorSelector.matchesLayout("default", null));
        assertTrue(DefaultMirrorSelector.matchesLayout("default", ""));
        assertTrue(DefaultMirrorSelector.matchesLayout("default", "*"));

        assertTrue(DefaultMirrorSelector.matchesLayout("default", "default"));
        assertFalse(DefaultMirrorSelector.matchesLayout("default", "legacy"));

        assertTrue(DefaultMirrorSelector.matchesLayout("default", "legacy,default"));
        assertTrue(DefaultMirrorSelector.matchesLayout("default", "default,legacy"));

        assertFalse(DefaultMirrorSelector.matchesLayout("default", "legacy,!default"));
        assertFalse(DefaultMirrorSelector.matchesLayout("default", "!default,legacy"));

        assertFalse(DefaultMirrorSelector.matchesLayout("default", "*,!default"));
        assertFalse(DefaultMirrorSelector.matchesLayout("default", "!default,*"));
    }

    public void testMirrorLayoutConsideredForMatching() {
        ArtifactRepository repo = getRepo("a");

        Mirror mirrorA = newMirror("a", "a", null, "http://a");
        Mirror mirrorB = newMirror("b", "a", "p2", "http://b");

        Mirror mirrorC = newMirror("c", "*", null, "http://c");
        Mirror mirrorD = newMirror("d", "*", "p2", "http://d");

        assertSame(mirrorA, mirrorSelector.getMirror(repo, Arrays.asList(mirrorA)));
        assertNull(mirrorSelector.getMirror(repo, Arrays.asList(mirrorB)));

        assertSame(mirrorC, mirrorSelector.getMirror(repo, Arrays.asList(mirrorC)));
        assertNull(mirrorSelector.getMirror(repo, Arrays.asList(mirrorD)));
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
