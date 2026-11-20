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
package org.apache.maven.it;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies origin-independent reuse across real Maven invocations, without plugin or network dependencies. */
class MavenITLocalRepositoryOriginPolicyTest extends AbstractMavenIntegrationTestCase {
    @Test
    void reusesReleaseOfflineAndRetainsStrictOptOut() throws Exception {
        final var directory = extractResources("local-repository-origin-policy");
        final var cache = directory.resolve("cache");
        invoke(directory, cache, "warm", "old", false, List.of()).execute();
        final var tracking = cache.resolve("org/apache/maven/its/origin/parent/1/_remote.repositories");
        assertTrue(Files.isRegularFile(tracking));
        final var before = Files.readString(tracking);
        final var reuse = invoke(directory, cache, "reuse", "new", true, List.of());
        reuse.execute();
        reuse.verifyErrorFreeLog();
        assertEquals(before, Files.readString(tracking));
        final var strict = invoke(
                directory,
                cache,
                "strict",
                "new",
                true,
                List.of(
                        "-Dmaven.repo.local.ignoreAvailability=false",
                        "-Dmaven.repo.local.tail.ignoreAvailability=false"));
        assertThrows(VerificationException.class, strict::execute);
        strict.verifyTextInLog("Non-resolvable parent POM");
    }

    @Test
    void preservesSplitRepositoryDefault() throws Exception {
        final var directory = extractResources("local-repository-origin-policy");
        final var cache = directory.resolve("split-cache");
        final var split =
                List.of("-Daether.lrm.enhanced.split=true", "-Daether.lrm.enhanced.splitRemoteRepository=false");
        invoke(directory, cache, "split-warm", "old", false, split).execute();
        final var strict = invoke(directory, cache, "split-strict", "new", true, split);
        assertThrows(VerificationException.class, strict::execute);
        strict.verifyTextInLog("Non-resolvable parent POM");
        final var override = invoke(
                directory,
                cache,
                "split-override",
                "new",
                true,
                List.of(
                        "-Daether.lrm.enhanced.split=true",
                        "-Daether.lrm.enhanced.splitRemoteRepository=false",
                        "-Dmaven.repo.local.ignoreAvailability=true"));
        override.execute();
        override.verifyErrorFreeLog();
    }

    @Test
    void reusesChainHeadAndTailAndHonorsStrictTailPolicy() throws Exception {
        final var directory = extractResources("local-repository-origin-policy");
        final var cache = directory.resolve("chain-cache");
        invoke(directory, cache, "chain-warm", "old", false, List.of()).execute();
        // A populated head must win even with an empty main repository behind it.
        final var head = invoke(
                directory,
                directory.resolve("empty-main"),
                "head",
                "new",
                true,
                List.of("-Dmaven.repo.local.head=" + cache));
        head.execute();
        head.verifyErrorFreeLog();
        final var tail = invoke(
                directory,
                directory.resolve("empty-head"),
                "tail",
                "new",
                true,
                List.of("-Dmaven.repo.local.tail=" + cache));
        tail.execute();
        tail.verifyErrorFreeLog();
        final var strict = invoke(
                directory,
                directory.resolve("empty-head"),
                "tail-strict",
                "new",
                true,
                List.of("-Dmaven.repo.local.tail=" + cache, "-Dmaven.repo.local.tail.ignoreAvailability=false"));
        assertThrows(VerificationException.class, strict::execute);
        strict.verifyTextInLog("Non-resolvable parent POM");
        final var override = invoke(
                directory,
                directory.resolve("empty-head"),
                "tail-override",
                "new",
                true,
                List.of(
                        "-Dmaven.repo.local.tail=" + cache,
                        "-Dmaven.repo.local.tail.ignoreAvailability=false",
                        "-Dmaven.repo.local.ignoreAvailability=true"));
        override.execute();
        override.verifyErrorFreeLog();
    }

    private Verifier invoke(
            final Path directory,
            final Path cache,
            final String log,
            final String origin,
            final boolean offline,
            final List<String> properties)
            throws VerificationException {
        final var verifier = newVerifier(directory);
        verifier.setAutoclean(false);
        verifier.setLogFileName(log + ".txt");
        verifier.addCliArgument("-Dmaven.repo.local=" + cache);
        verifier.addCliArgument("-Dorigin.id=" + origin);
        verifier.addCliArgument("-Dorigin.url=" + directory.resolve("remote").toUri());
        for (final var property : properties) {
            verifier.addCliArgument(property);
        }
        if (offline) {
            verifier.addCliArgument("--offline");
        }
        verifier.addCliArgument("validate");
        return verifier;
    }
}
