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
package org.apache.maven.cling.invoker.mvn;

import java.nio.file.Path;
import java.util.Map;

import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.cisupport.CIInfo;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for CI optimizations in Maven.
 */
class CIOptimizationsIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testCIOptimizationsAppliedWhenCIDetected() throws Exception {
        // Given: A mock CI environment
        CIInfo mockCIInfo = mock(CIInfo.class);
        when(mockCIInfo.name()).thenReturn("TestCI");
        when(mockCIInfo.message()).thenReturn("Test CI detected");

        // Create a mock InvokerRequest with CI info
        InvokerRequest mockRequest = mock(InvokerRequest.class);
        when(mockRequest.ciInfo()).thenReturn(java.util.Optional.of(mockCIInfo));
        when(mockRequest.topDirectory()).thenReturn(tempDir);
        when(mockRequest.rootDirectory()).thenReturn(java.util.Optional.of(tempDir));
        when(mockRequest.userProperties()).thenReturn(Map.of());
        when(mockRequest.systemProperties()).thenReturn(Map.of());
        when(mockRequest.cwd()).thenReturn(tempDir);
        when(mockRequest.installationDirectory()).thenReturn(tempDir);
        when(mockRequest.userHomeDirectory()).thenReturn(tempDir);
        org.apache.maven.api.cli.ParserRequest mockParserRequest = mock(org.apache.maven.api.cli.ParserRequest.class);
        when(mockParserRequest.logger()).thenReturn(mock(org.apache.maven.api.cli.Logger.class));
        when(mockRequest.parserRequest()).thenReturn(mockParserRequest);

        // Create mock options without explicit CI settings
        MavenOptions mockOptions = mock(MavenOptions.class);
        when(mockOptions.showVersion()).thenReturn(java.util.Optional.empty());
        when(mockOptions.showErrors()).thenReturn(java.util.Optional.empty());
        when(mockOptions.nonInteractive()).thenReturn(java.util.Optional.empty());
        when(mockOptions.forceInteractive()).thenReturn(java.util.Optional.empty());
        when(mockOptions.source()).thenReturn("CLI");
        when(mockRequest.options()).thenReturn(java.util.Optional.of(mockOptions));

        // When: Creating MavenInvoker and context
        MavenInvoker invoker = new MavenInvoker(ProtoLookup.builder().build(), null);
        MavenContext context = invoker.createContext(mockRequest);

        // Then: Options should be CI-aware
        assertInstanceOf(CIAwareMavenOptions.class, context.options());
        CIAwareMavenOptions ciOptions = (CIAwareMavenOptions) context.options();

        // Verify CI optimizations are applied
        assertTrue(ciOptions.showVersion().orElse(false), "showVersion should be enabled in CI");
        assertTrue(ciOptions.showErrors().orElse(false), "showErrors should be enabled in CI");
        assertTrue(ciOptions.nonInteractive().orElse(false), "nonInteractive should be enabled in CI");

        // Verify CI info is preserved
        assertSame(mockCIInfo, ciOptions.getCIInfo());
    }

    @Test
    void testCIOptimizationsNotAppliedWhenNoCIDetected() throws Exception {
        // Given: No CI environment
        InvokerRequest mockRequest = mock(InvokerRequest.class);
        when(mockRequest.ciInfo()).thenReturn(java.util.Optional.empty());
        when(mockRequest.topDirectory()).thenReturn(tempDir);
        when(mockRequest.rootDirectory()).thenReturn(java.util.Optional.of(tempDir));
        when(mockRequest.userProperties()).thenReturn(Map.of());
        when(mockRequest.systemProperties()).thenReturn(Map.of());
        when(mockRequest.cwd()).thenReturn(tempDir);
        when(mockRequest.installationDirectory()).thenReturn(tempDir);
        when(mockRequest.userHomeDirectory()).thenReturn(tempDir);
        org.apache.maven.api.cli.ParserRequest mockParserRequest2 = mock(org.apache.maven.api.cli.ParserRequest.class);
        when(mockParserRequest2.logger()).thenReturn(mock(org.apache.maven.api.cli.Logger.class));
        when(mockRequest.parserRequest()).thenReturn(mockParserRequest2);

        // Create mock options
        MavenOptions mockOptions = mock(MavenOptions.class);
        when(mockOptions.showVersion()).thenReturn(java.util.Optional.empty());
        when(mockOptions.showErrors()).thenReturn(java.util.Optional.empty());
        when(mockOptions.nonInteractive()).thenReturn(java.util.Optional.empty());
        when(mockOptions.source()).thenReturn("CLI");
        when(mockRequest.options()).thenReturn(java.util.Optional.of(mockOptions));

        // When: Creating MavenInvoker and context
        MavenInvoker invoker = new MavenInvoker(ProtoLookup.builder().build(), null);
        MavenContext context = invoker.createContext(mockRequest);

        // Then: Options should NOT be CI-aware
        assertFalse(
                context.options() instanceof CIAwareMavenOptions,
                "Options should not be CI-aware when no CI is detected");
        assertSame(mockOptions, context.options());
    }

    @Test
    void testExplicitOptionsOverrideCIDefaults() throws Exception {
        // Given: A mock CI environment
        CIInfo mockCIInfo = mock(CIInfo.class);
        when(mockCIInfo.name()).thenReturn("TestCI");

        InvokerRequest mockRequest = mock(InvokerRequest.class);
        when(mockRequest.ciInfo()).thenReturn(java.util.Optional.of(mockCIInfo));
        when(mockRequest.topDirectory()).thenReturn(tempDir);
        when(mockRequest.rootDirectory()).thenReturn(java.util.Optional.of(tempDir));
        when(mockRequest.userProperties()).thenReturn(Map.of());
        when(mockRequest.systemProperties()).thenReturn(Map.of());
        when(mockRequest.cwd()).thenReturn(tempDir);
        when(mockRequest.installationDirectory()).thenReturn(tempDir);
        when(mockRequest.userHomeDirectory()).thenReturn(tempDir);
        org.apache.maven.api.cli.ParserRequest mockParserRequest3 = mock(org.apache.maven.api.cli.ParserRequest.class);
        when(mockParserRequest3.logger()).thenReturn(mock(org.apache.maven.api.cli.Logger.class));
        when(mockRequest.parserRequest()).thenReturn(mockParserRequest3);

        // Create mock options with explicit settings that contradict CI defaults
        MavenOptions mockOptions = mock(MavenOptions.class);
        when(mockOptions.showVersion()).thenReturn(java.util.Optional.of(false)); // explicit false
        when(mockOptions.showErrors()).thenReturn(java.util.Optional.of(false)); // explicit false
        when(mockOptions.nonInteractive()).thenReturn(java.util.Optional.of(false)); // explicit false
        when(mockOptions.forceInteractive()).thenReturn(java.util.Optional.empty());
        when(mockOptions.source()).thenReturn("CLI");
        when(mockRequest.options()).thenReturn(java.util.Optional.of(mockOptions));

        // When: Creating MavenInvoker and context
        MavenInvoker invoker = new MavenInvoker(ProtoLookup.builder().build(), null);
        MavenContext context = invoker.createContext(mockRequest);

        // Then: Explicit user choices should be respected
        CIAwareMavenOptions ciOptions = (CIAwareMavenOptions) context.options();
        assertFalse(ciOptions.showVersion().orElse(true), "Explicit showVersion=false should be respected");
        assertFalse(ciOptions.showErrors().orElse(true), "Explicit showErrors=false should be respected");
        assertFalse(ciOptions.nonInteractive().orElse(true), "Explicit nonInteractive=false should be respected");
    }

    @Test
    void testForceInteractiveOverridesCIDefaults() throws Exception {
        // Given: A mock CI environment with force-interactive
        CIInfo mockCIInfo = mock(CIInfo.class);
        when(mockCIInfo.name()).thenReturn("TestCI");

        InvokerRequest mockRequest = mock(InvokerRequest.class);
        when(mockRequest.ciInfo()).thenReturn(java.util.Optional.of(mockCIInfo));
        when(mockRequest.topDirectory()).thenReturn(tempDir);
        when(mockRequest.rootDirectory()).thenReturn(java.util.Optional.of(tempDir));
        when(mockRequest.userProperties()).thenReturn(Map.of());
        when(mockRequest.systemProperties()).thenReturn(Map.of());
        when(mockRequest.cwd()).thenReturn(tempDir);
        when(mockRequest.installationDirectory()).thenReturn(tempDir);
        when(mockRequest.userHomeDirectory()).thenReturn(tempDir);
        org.apache.maven.api.cli.ParserRequest mockParserRequest4 = mock(org.apache.maven.api.cli.ParserRequest.class);
        when(mockParserRequest4.logger()).thenReturn(mock(org.apache.maven.api.cli.Logger.class));
        when(mockRequest.parserRequest()).thenReturn(mockParserRequest4);

        // Create mock options with force-interactive
        MavenOptions mockOptions = mock(MavenOptions.class);
        when(mockOptions.showVersion()).thenReturn(java.util.Optional.empty());
        when(mockOptions.showErrors()).thenReturn(java.util.Optional.empty());
        when(mockOptions.nonInteractive()).thenReturn(java.util.Optional.empty());
        when(mockOptions.forceInteractive()).thenReturn(java.util.Optional.of(true)); // force interactive
        when(mockOptions.source()).thenReturn("CLI");
        when(mockRequest.options()).thenReturn(java.util.Optional.of(mockOptions));

        // When: Creating MavenInvoker and context
        MavenInvoker invoker = new MavenInvoker(ProtoLookup.builder().build(), null);
        MavenContext context = invoker.createContext(mockRequest);

        // Then: CI optimizations should still be applied (force-interactive affects interactive mode, not these
        // options)
        CIAwareMavenOptions ciOptions = (CIAwareMavenOptions) context.options();
        assertTrue(ciOptions.showVersion().orElse(false), "showVersion should still be enabled in CI");
        assertTrue(ciOptions.showErrors().orElse(false), "showErrors should still be enabled in CI");
        assertTrue(ciOptions.nonInteractive().orElse(false), "nonInteractive should still be enabled in CI");
        assertTrue(ciOptions.forceInteractive().orElse(false), "forceInteractive should be preserved");
    }
}
