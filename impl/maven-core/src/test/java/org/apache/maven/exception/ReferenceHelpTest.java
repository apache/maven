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
package org.apache.maven.exception;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceHelpTest {

    private static final List<String> BUILT_IN_KEYS = List.of(
            "MojoExecutionException",
            "ConnectException",
            "LinkageError",
            "PluginExecutionException",
            "AetherClassNotFound",
            "PluginContainerException",
            "LifecycleExecutionException",
            "RemoteRepositoryFiltering");

    @Test
    void normalizeKeyHandlesPlainKeys() {
        assertEquals("AetherClassNotFound", ReferenceHelp.normalizeKey("AetherClassNotFound"));
    }

    @Test
    void normalizeKeyHandlesCwikiUrls() {
        assertEquals(
                "AetherClassNotFound",
                ReferenceHelp.normalizeKey("http://cwiki.apache.org/confluence/display/MAVEN/AetherClassNotFound"));
    }

    @Test
    void normalizeKeyHandlesArbitraryUrls() {
        assertEquals(
                "AetherClassNotFound",
                ReferenceHelp.normalizeKey("https://maven.apache.org/resolver/AetherClassNotFound"));
        assertEquals("MyError", ReferenceHelp.normalizeKey("https://example.com/errors/MyError?x=1"));
    }

    @Test
    void normalizeKeyRejectsInvalidKeys() {
        assertNull(ReferenceHelp.normalizeKey(null));
        assertNull(ReferenceHelp.normalizeKey(""));
        assertNull(ReferenceHelp.normalizeKey("http://cwiki.apache.org/confluence/display/MAVEN/"));
        assertNull(ReferenceHelp.normalizeKey("a/b"));
        assertNull(ReferenceHelp.normalizeKey("a b"));
    }

    @Test
    void everyBuiltInReferenceHasHelpText() {
        for (String key : BUILT_IN_KEYS) {
            String help = ReferenceHelp.getHelpText(key);
            assertNotNull(help, "Expected help text for " + key);
            assertTrue(!help.isBlank(), "Expected non-blank help text for " + key);
        }
    }

    @Test
    void cwikiStyleReferencesResolveToSameHelpText() {
        for (String key : BUILT_IN_KEYS) {
            String plain = ReferenceHelp.getHelpText(key);
            String url = ReferenceHelp.getHelpText("http://cwiki.apache.org/confluence/display/MAVEN/" + key);
            assertEquals(plain, url, "Help text should not depend on reference form for " + key);
        }
    }

    @Test
    void unknownReferencesProduceNoHelpText() {
        assertNull(ReferenceHelp.getHelpText("NoSuchException"));
        assertNull(ReferenceHelp.getHelpText("com.example.SomeError"));
        assertNull(ReferenceHelp.getHelpText(""));
        assertNull(ReferenceHelp.getHelpText(null));
    }

    @Test
    void hasHelpTextReturnsTrueForBuiltInReferences() {
        for (String key : BUILT_IN_KEYS) {
            assertTrue(ReferenceHelp.hasHelpText(key), "Expected help text resource for " + key);
            assertTrue(
                    ReferenceHelp.hasHelpText("http://cwiki.apache.org/confluence/display/MAVEN/" + key),
                    "Expected help text resource for cwiki URL of " + key);
        }
        assertTrue(ReferenceHelp.hasHelpText(
                "https://maven.apache.org/resolver/remote-repository-filtering.html"));
    }

    @Test
    void hasHelpTextReturnsFalseForUnknownOrInvalidReferences() {
        assertFalse(ReferenceHelp.hasHelpText("NoSuchException"));
        assertFalse(ReferenceHelp.hasHelpText("com.example.SomeError"));
        assertFalse(ReferenceHelp.hasHelpText("a b"));
        assertFalse(ReferenceHelp.hasHelpText(""));
        assertFalse(ReferenceHelp.hasHelpText(null));
    }

    @Test
    void hasHelpTextAgreesWithGetHelpTextForBuiltIns() {
        for (String key : BUILT_IN_KEYS) {
            assertEquals(ReferenceHelp.getHelpText(key) != null, ReferenceHelp.hasHelpText(key), key);
        }
    }

    @Test
    void helpTextIsTrimmed() {
        String help = ReferenceHelp.getHelpText("AetherClassNotFound");
        assertNotNull(help);
        assertEquals(help, help.strip());
        assertTrue(help.contains("Sonatype Aether"), "Help text should mention Sonatype Aether");
    }

    @Test
    void remoteRepositoryFilteringUrlResolvesToHelpText() {
        String help = ReferenceHelp.getHelpText("https://maven.apache.org/resolver/remote-repository-filtering.html");
        assertNotNull(help);
        assertTrue(
                help.contains("https://maven.apache.org/resolver/remote-repository-filtering.html"),
                "Help text should reference the remote repository filtering documentation");
    }
}
