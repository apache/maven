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

import java.util.Optional;

import org.apache.maven.api.cli.cisupport.CIInfo;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CIAwareMavenOptions}.
 */
class CIAwareMavenOptionsTest {

    private final CIInfo mockCIInfo = mock(CIInfo.class);

    @Test
    void testCIOptimizationsAppliedWhenNotExplicitlySet() {
        // Given: A delegate with no explicit CI-related options set
        MavenOptions delegate = mock(MavenOptions.class);
        when(delegate.showVersion()).thenReturn(Optional.empty());
        when(delegate.showErrors()).thenReturn(Optional.empty());
        when(delegate.nonInteractive()).thenReturn(Optional.empty());
        when(delegate.source()).thenReturn("test");

        // When: Creating CI-aware options
        CIAwareMavenOptions ciOptions = new CIAwareMavenOptions(delegate, mockCIInfo);

        // Then: CI optimizations should be applied
        assertTrue(ciOptions.showVersion().orElse(false), "showVersion should be enabled in CI");
        assertTrue(ciOptions.showErrors().orElse(false), "showErrors should be enabled in CI");
        assertTrue(ciOptions.nonInteractive().orElse(false), "nonInteractive should be enabled in CI");
    }

    @Test
    void testExplicitOptionsRespected() {
        // Given: A delegate with explicit options set
        MavenOptions delegate = mock(MavenOptions.class);
        when(delegate.showVersion()).thenReturn(Optional.of(false));
        when(delegate.showErrors()).thenReturn(Optional.of(false));
        when(delegate.nonInteractive()).thenReturn(Optional.of(false));
        when(delegate.source()).thenReturn("test");

        // When: Creating CI-aware options
        CIAwareMavenOptions ciOptions = new CIAwareMavenOptions(delegate, mockCIInfo);

        // Then: Explicit user choices should be respected
        assertFalse(ciOptions.showVersion().orElse(true), "Explicit showVersion=false should be respected");
        assertFalse(ciOptions.showErrors().orElse(true), "Explicit showErrors=false should be respected");
        assertFalse(ciOptions.nonInteractive().orElse(true), "Explicit nonInteractive=false should be respected");
    }

    @Test
    void testMixedExplicitAndDefaultOptions() {
        // Given: A delegate with some explicit options and some defaults
        MavenOptions delegate = mock(MavenOptions.class);
        when(delegate.showVersion()).thenReturn(Optional.of(false)); // explicit
        when(delegate.showErrors()).thenReturn(Optional.empty()); // default
        when(delegate.nonInteractive()).thenReturn(Optional.empty()); // default
        when(delegate.source()).thenReturn("test");

        // When: Creating CI-aware options
        CIAwareMavenOptions ciOptions = new CIAwareMavenOptions(delegate, mockCIInfo);

        // Then: Mix of explicit and CI defaults
        assertFalse(ciOptions.showVersion().orElse(true), "Explicit showVersion=false should be respected");
        assertTrue(ciOptions.showErrors().orElse(false), "showErrors should default to true in CI");
        assertTrue(ciOptions.nonInteractive().orElse(false), "nonInteractive should default to true in CI");
    }

    @Test
    void testDelegationOfOtherMethods() {
        // Given: A delegate with various options
        MavenOptions delegate = mock(MavenOptions.class);
        when(delegate.quiet()).thenReturn(Optional.of(true));
        when(delegate.verbose()).thenReturn(Optional.of(false));
        when(delegate.offline()).thenReturn(Optional.of(true));
        when(delegate.source()).thenReturn("test");

        // When: Creating CI-aware options
        CIAwareMavenOptions ciOptions = new CIAwareMavenOptions(delegate, mockCIInfo);

        // Then: Other methods should be delegated unchanged
        assertEquals(Optional.of(true), ciOptions.quiet());
        assertEquals(Optional.of(false), ciOptions.verbose());
        assertEquals(Optional.of(true), ciOptions.offline());
    }

    @Test
    void testSourceDescription() {
        // Given: A delegate with a source
        MavenOptions delegate = mock(MavenOptions.class);
        when(delegate.source()).thenReturn("CLI");

        // When: Creating CI-aware options
        CIAwareMavenOptions ciOptions = new CIAwareMavenOptions(delegate, mockCIInfo);

        // Then: Source should indicate CI-aware wrapper
        assertEquals("CI-aware(CLI)", ciOptions.source());
    }

    @Test
    void testGetCIInfo() {
        // Given: A delegate and CI info
        MavenOptions delegate = mock(MavenOptions.class);
        when(delegate.source()).thenReturn("test");

        // When: Creating CI-aware options
        CIAwareMavenOptions ciOptions = new CIAwareMavenOptions(delegate, mockCIInfo);

        // Then: CI info should be accessible
        assertSame(mockCIInfo, ciOptions.getCIInfo());
    }

    @Test
    void testGetDelegate() {
        // Given: A delegate
        MavenOptions delegate = mock(MavenOptions.class);
        when(delegate.source()).thenReturn("test");

        // When: Creating CI-aware options
        CIAwareMavenOptions ciOptions = new CIAwareMavenOptions(delegate, mockCIInfo);

        // Then: Delegate should be accessible
        assertSame(delegate, ciOptions.getDelegate());
    }

    @Test
    void testInterpolate() {
        // Given: A delegate that supports interpolation
        MavenOptions delegate = mock(MavenOptions.class);
        MavenOptions interpolatedDelegate = mock(MavenOptions.class);
        when(delegate.source()).thenReturn("test");
        when(delegate.interpolate(any())).thenReturn(interpolatedDelegate);
        when(interpolatedDelegate.source()).thenReturn("interpolated");

        // When: Creating CI-aware options and interpolating
        CIAwareMavenOptions ciOptions = new CIAwareMavenOptions(delegate, mockCIInfo);
        MavenOptions interpolated = ciOptions.interpolate(s -> s);

        // Then: Should return new CI-aware options with interpolated delegate
        assertInstanceOf(CIAwareMavenOptions.class, interpolated);
        CIAwareMavenOptions ciInterpolated = (CIAwareMavenOptions) interpolated;
        assertSame(interpolatedDelegate, ciInterpolated.getDelegate());
        assertSame(mockCIInfo, ciInterpolated.getCIInfo());
    }
}
