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

import org.apache.maven.api.settings.PhaseInjection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SettingsLifecycleProcessor#validate(PhaseInjection)}.
 */
class SettingsLifecycleProcessorTest {

    @Test
    void validPhaseWithAfterOnly() {
        PhaseInjection phase =
                PhaseInjection.newBuilder().name("my-phase").after("compile").build();
        assertDoesNotThrow(() -> SettingsLifecycleProcessor.validate(phase));
    }

    @Test
    void validPhaseWithBeforeOnly() {
        PhaseInjection phase =
                PhaseInjection.newBuilder().name("my-phase").before("test").build();
        assertDoesNotThrow(() -> SettingsLifecycleProcessor.validate(phase));
    }

    @Test
    void validPhaseWithParentOnly() {
        PhaseInjection phase =
                PhaseInjection.newBuilder().name("my-phase").parent("default").build();
        assertDoesNotThrow(() -> SettingsLifecycleProcessor.validate(phase));
    }

    @Test
    void validPhaseWithAfterAndBefore() {
        PhaseInjection phase = PhaseInjection.newBuilder()
                .name("my-phase")
                .after("compile")
                .before("test")
                .build();
        assertDoesNotThrow(() -> SettingsLifecycleProcessor.validate(phase));
    }

    @Test
    void validPhaseWithParentAndAfter() {
        PhaseInjection phase = PhaseInjection.newBuilder()
                .name("my-phase")
                .parent("default")
                .after("compile")
                .build();
        assertDoesNotThrow(() -> SettingsLifecycleProcessor.validate(phase));
    }

    @Test
    void missingNameThrows() {
        PhaseInjection phase = PhaseInjection.newBuilder().after("compile").build();
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> SettingsLifecycleProcessor.validate(phase));
        assertTrue(ex.getMessage().contains("non-blank 'name'"));
    }

    @Test
    void blankNameThrows() {
        PhaseInjection phase =
                PhaseInjection.newBuilder().name("   ").after("compile").build();
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> SettingsLifecycleProcessor.validate(phase));
        assertTrue(ex.getMessage().contains("non-blank 'name'"));
    }

    @Test
    void noAnchorThrows() {
        PhaseInjection phase = PhaseInjection.newBuilder().name("my-phase").build();
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> SettingsLifecycleProcessor.validate(phase));
        assertTrue(ex.getMessage().contains("anchor it in the lifecycle DAG"));
    }

    @Test
    void blankAfterAndBeforeWithNoParentThrows() {
        PhaseInjection phase = PhaseInjection.newBuilder()
                .name("my-phase")
                .after("  ")
                .before("")
                .build();
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> SettingsLifecycleProcessor.validate(phase));
        assertTrue(ex.getMessage().contains("anchor it in the lifecycle DAG"));
    }
}
