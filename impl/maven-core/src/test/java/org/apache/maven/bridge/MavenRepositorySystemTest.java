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
package org.apache.maven.bridge;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenRepositorySystemTest {

    @Test
    void testExternalHttpRepoMatchesDavProtocols() {
        assertTrue(MavenRepositorySystem.isExternalHttpRepo(repo("http://repo.example.com/m2/")));
        assertTrue(MavenRepositorySystem.isExternalHttpRepo(repo("dav:http://repo.example.com/m2/")));
        assertTrue(MavenRepositorySystem.isExternalHttpRepo(repo("dav+http://repo.example.com/m2/")));
        assertTrue(MavenRepositorySystem.isExternalHttpRepo(repo("dav://repo.example.com/m2/")));
    }

    @Test
    void testExternalHttpRepoClassificationForNonHttpUrls() {
        assertFalse(MavenRepositorySystem.isExternalHttpRepo(repo("https://repo.example.com/m2/")));
        assertFalse(MavenRepositorySystem.isExternalHttpRepo(repo("http://localhost:8080/m2/")));
        assertFalse(MavenRepositorySystem.isExternalHttpRepo(repo("http://127.0.0.1/m2/")));
        assertFalse(MavenRepositorySystem.isExternalHttpRepo(repo("file:///tmp/repo")));
    }

    @Test
    void testUnparseableUrlClassification() {
        assertTrue(MavenRepositorySystem.isExternalRepo(repo("not a url")));
        assertTrue(MavenRepositorySystem.isExternalHttpRepo(repo("not a url")));
    }

    @Test
    void testExternalRepoClassification() {
        assertTrue(MavenRepositorySystem.isExternalRepo(repo("https://repo.example.com/m2/")));
        assertTrue(MavenRepositorySystem.isExternalRepo(repo("dav:http://repo.example.com/m2/")));
        assertFalse(MavenRepositorySystem.isExternalRepo(repo("file:///tmp/repo")));
        assertFalse(MavenRepositorySystem.isExternalRepo(repo("http://localhost/m2/")));
        assertFalse(MavenRepositorySystem.isExternalRepo(repo("http://127.0.0.1/m2/")));
    }

    @Test
    void testLoopbackAddressesAreLocal() {
        for (String url : new String[] {
            "http://127.0.0.2/m2/",
            "http://127.255.255.254:8080/m2/",
            "http://[::1]/m2/",
            "http://[::1]:8080/m2/",
            "http://[0:0:0:0:0:0:0:1]:8080/m2/",
            // an IPv4 loopback address written as IPv4-mapped IPv6 address
            "http://[::ffff:127.0.0.1]/m2/",
            "http://[::ffff:7f00:1]:8080/m2/"
        }) {
            assertFalse(MavenRepositorySystem.isExternalRepo(repo(url)), url);
            assertFalse(MavenRepositorySystem.isExternalHttpRepo(repo(url)), url);
        }
    }

    @Test
    void testOtherAddressesAreExternal() {
        for (String url : new String[] {
            "http://128.0.0.1/m2/",
            "http://127.0.0.1.example.com/m2/",
            "http://127.example.com/m2/",
            "http://[2001:db8::1]/m2/",
            "http://[2001:db8::1]:8080/m2/",
            // an IPv4-mapped IPv6 address which is not a loopback one
            "http://[::ffff:8.8.8.8]/m2/"
        }) {
            assertTrue(MavenRepositorySystem.isExternalRepo(repo(url)), url);
            assertTrue(MavenRepositorySystem.isExternalHttpRepo(repo(url)), url);
        }
    }

    private static ArtifactRepository repo(String url) {
        return new MavenArtifactRepository("test", url, null, null, null);
    }
}
