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
package org.apache.maven.settings.merge;

import java.util.List;

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.TrackableBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenSettingsMergerTest {

    private MavenSettingsMerger merger;

    @BeforeEach
    void setUp() {
        merger = new MavenSettingsMerger();
    }

    /**
     * Verifies that mirrors defined in dominant settings (e.g., user settings)
     * precede mirrors in recessive settings (e.g., global settings).
     * This ensures user-defined insecure repository mirrors take precedence
     * over maven-default-http-blocker (MNG-7841).
     */
    @Test
    void testDominantMirrorsPrecedeRecessiveMirrors() {
        Settings dominant = new Settings();
        Mirror userMirror = new Mirror();
        userMirror.setId("user-mirror");
        userMirror.setMirrorOf("insecure-repo");
        userMirror.setUrl("http://insecure.example.com/");
        dominant.addMirror(userMirror);

        Settings recessive = new Settings();
        Mirror defaultBlocker = new Mirror();
        defaultBlocker.setId("maven-default-http-blocker");
        defaultBlocker.setMirrorOf("external:http:*");
        defaultBlocker.setUrl("http://0.0.0.0/");
        defaultBlocker.setBlocked(true);
        recessive.addMirror(defaultBlocker);

        merger.merge(dominant, recessive, TrackableBase.GLOBAL_LEVEL);

        List<Mirror> mirrors = dominant.getMirrors();
        assertEquals(2, mirrors.size());
        assertEquals("user-mirror", mirrors.get(0).getId());
        assertEquals("maven-default-http-blocker", mirrors.get(1).getId());
        assertTrue(mirrors.get(1).isBlocked());
    }

    @Test
    void testDominantMirrorOverridesRecessiveWithSameId() {
        Settings dominant = new Settings();
        Mirror userMirror = new Mirror();
        userMirror.setId("maven-default-http-blocker");
        userMirror.setMirrorOf("external:http:*");
        userMirror.setUrl("http://allowed.example.com/");
        userMirror.setBlocked(false);
        dominant.addMirror(userMirror);

        Settings recessive = new Settings();
        Mirror defaultBlocker = new Mirror();
        defaultBlocker.setId("maven-default-http-blocker");
        defaultBlocker.setMirrorOf("external:http:*");
        defaultBlocker.setUrl("http://0.0.0.0/");
        defaultBlocker.setBlocked(true);
        recessive.addMirror(defaultBlocker);

        merger.merge(dominant, recessive, TrackableBase.GLOBAL_LEVEL);

        List<Mirror> mirrors = dominant.getMirrors();
        assertEquals(1, mirrors.size());
        assertEquals("maven-default-http-blocker", mirrors.get(0).getId());
        assertEquals("http://allowed.example.com/", mirrors.get(0).getUrl());
        assertFalse(mirrors.get(0).isBlocked());
    }

    @Test
    void testDominantPrecedenceForIdentifiables() {
        Settings dominant = new Settings();
        Server dominantServer = new Server();
        dominantServer.setId("server-1");
        dominant.addServer(dominantServer);

        Proxy dominantProxy = new Proxy();
        dominantProxy.setId("proxy-1");
        dominant.addProxy(dominantProxy);

        Profile dominantProfile = new Profile();
        dominantProfile.setId("profile-1");
        dominant.addProfile(dominantProfile);

        Repository dominantRepo = new Repository();
        dominantRepo.setId("repo-1");
        dominant.addRepository(dominantRepo);

        Settings recessive = new Settings();
        Server recessiveServer = new Server();
        recessiveServer.setId("server-2");
        recessive.addServer(recessiveServer);

        Proxy recessiveProxy = new Proxy();
        recessiveProxy.setId("proxy-2");
        recessive.addProxy(recessiveProxy);

        Profile recessiveProfile = new Profile();
        recessiveProfile.setId("profile-2");
        recessive.addProfile(recessiveProfile);

        Repository recessiveRepo = new Repository();
        recessiveRepo.setId("repo-2");
        recessive.addRepository(recessiveRepo);

        merger.merge(dominant, recessive, TrackableBase.GLOBAL_LEVEL);

        assertEquals("server-1", dominant.getServers().get(0).getId());
        assertEquals("server-2", dominant.getServers().get(1).getId());

        assertEquals("proxy-1", dominant.getProxies().get(0).getId());
        assertEquals("proxy-2", dominant.getProxies().get(1).getId());

        assertEquals("profile-1", dominant.getProfiles().get(0).getId());
        assertEquals("profile-2", dominant.getProfiles().get(1).getId());

        assertEquals("repo-1", dominant.getRepositories().get(0).getId());
        assertEquals("repo-2", dominant.getRepositories().get(1).getId());
    }

    @Test
    void testLocalRepositoryMerging() {
        Settings dominant = new Settings();
        dominant.setLocalRepository("/dominant/path");

        Settings recessive = new Settings();
        recessive.setLocalRepository("/recessive/path");

        merger.merge(dominant, recessive, TrackableBase.GLOBAL_LEVEL);
        assertEquals("/dominant/path", dominant.getLocalRepository());

        Settings dominantEmpty = new Settings();
        Settings recessive2 = new Settings();
        recessive2.setLocalRepository("/recessive/path");
        merger.merge(dominantEmpty, recessive2, TrackableBase.GLOBAL_LEVEL);
        assertEquals("/recessive/path", dominantEmpty.getLocalRepository());
    }
}
