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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import eu.maveniverse.domtrip.Document;
import org.apache.maven.api.settings.Mirror;
import org.apache.maven.api.settings.Proxy;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the remote-resolution gate in {@link AbstractUpgradeStrategy}.
 *
 * <p>mvnup builds effective models with a standalone resolver session that cannot honor
 * mirrors, proxies or offline mode from the operator's settings. The gate must skip
 * remote-model-dependent work whenever such a posture is configured, instead of silently
 * resolving from public repositories.</p>
 */
@DisplayName("Remote resolution gate")
class RemoteResolutionGateTest {

    private static Settings settingsWithRedirectingMirror() {
        return Settings.newBuilder()
                .mirrors(List.of(Mirror.newBuilder()
                        .id("corp-mirror")
                        .mirrorOf("*")
                        .url("https://mirror.corp.example/maven2")
                        .build()))
                .build();
    }

    @Test
    @DisplayName("no reason when settings were never loaded (test/embedded use)")
    void reasonIsNullWhenSettingsWereNotLoaded() {
        UpgradeContext context = TestUtils.createMockContext();
        context.effectiveSettings = null;
        assertNull(AbstractUpgradeStrategy.remoteResolutionUnsupportedReason(context));
    }

    @Test
    @DisplayName("no reason for empty effective settings")
    void reasonIsNullForEmptySettings() {
        UpgradeContext context = TestUtils.createMockContext();
        context.effectiveSettings = Settings.newInstance();
        assertNull(AbstractUpgradeStrategy.remoteResolutionUnsupportedReason(context));
    }

    @Test
    @DisplayName("no reason when only blocked mirrors are configured (default http blocker)")
    void reasonIsNullWhenOnlyBlockedMirrorsAreConfigured() {
        UpgradeContext context = TestUtils.createMockContext();
        context.effectiveSettings = Settings.newBuilder()
                .mirrors(List.of(Mirror.newBuilder()
                        .id("maven-default-http-blocker")
                        .mirrorOf("external:http:*")
                        .url("http://0.0.0.0/")
                        .blocked(true)
                        .build()))
                .build();
        assertNull(AbstractUpgradeStrategy.remoteResolutionUnsupportedReason(context));
    }

    @Test
    @DisplayName("redirecting mirror disables remote resolution")
    void reasonIsNonNullForRedirectingMirror() {
        UpgradeContext context = TestUtils.createMockContext();
        context.effectiveSettings = settingsWithRedirectingMirror();
        assertNotNull(AbstractUpgradeStrategy.remoteResolutionUnsupportedReason(context));
    }

    @Test
    @DisplayName("active proxy disables remote resolution")
    void reasonIsNonNullForActiveProxy() {
        UpgradeContext context = TestUtils.createMockContext();
        context.effectiveSettings = Settings.newBuilder()
                .proxies(List.of(Proxy.newBuilder()
                        .id("corp-proxy")
                        .activeString("true")
                        .host("proxy.corp.example")
                        .build()))
                .build();
        assertNotNull(AbstractUpgradeStrategy.remoteResolutionUnsupportedReason(context));
    }

    @Test
    @DisplayName("offline mode disables remote resolution")
    void reasonIsNonNullForOfflineMode() {
        UpgradeContext context = TestUtils.createMockContext();
        context.effectiveSettings = Settings.newBuilder().offline(true).build();
        assertNotNull(AbstractUpgradeStrategy.remoteResolutionUnsupportedReason(context));
    }

    @Test
    @DisplayName("compatibility fixes must not comment out elements when the posture cannot be honored")
    void compatibilityFixDoesNotCommentOutWhenPostureCannotBeHonored() {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${guava-version}</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
            """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = TestUtils.createMockContext();
        // Operator routes all repository traffic through a mirror: the standalone resolver
        // cannot honor it, so no effective model is available and the property cannot be
        // proven undefined. The fix must be skipped rather than commenting out a possibly
        // valid dependency.
        context.effectiveSettings = settingsWithRedirectingMirror();

        CompatibilityFixStrategy strategy = new CompatibilityFixStrategy();
        UpgradeResult result = strategy.doApply(context, pomMap);

        assertTrue(result.success(), "Strategy should succeed while skipping the unverifiable fix");
        assertEquals(0, result.modifiedCount(), "No POM should be modified");

        String xml = DomUtils.toXml(document);
        assertFalse(xml.contains("mvnup: commented out"), "Nothing may be commented out");
        assertTrue(xml.contains("${guava-version}"), "The dependency must be left untouched");
    }
}
