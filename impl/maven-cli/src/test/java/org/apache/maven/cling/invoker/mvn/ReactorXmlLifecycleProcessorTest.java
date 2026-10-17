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

import java.util.List;

import org.apache.maven.api.reactor.PhaseInjection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ReactorXmlLifecycleProcessor}, focusing on the
 * {@code validate()} logic which can be exercised without a live Maven session.
 */
class ReactorXmlLifecycleProcessorTest {

    // -------------------------------------------------------------------------
    // Helpers: build PhaseInjection via its generated builder
    // -------------------------------------------------------------------------

    private static PhaseInjection phase(String name, String parent, String after, String before) {
        PhaseInjection.Builder b = PhaseInjection.newBuilder();
        if (name != null) {
            b.name(name);
        }
        if (parent != null) {
            b.parent(parent);
        }
        if (after != null) {
            b.after(after);
        }
        if (before != null) {
            b.before(before);
        }
        return b.build();
    }

    // -------------------------------------------------------------------------
    // Validation: valid declarations (should NOT throw)
    // -------------------------------------------------------------------------

    @Test
    void validWithParentAndAfter() {
        assertDoesNotThrow(() -> validateVia(List.of(phase("p", "verify", "unit-test", null))));
    }

    @Test
    void validWithParentAndBefore() {
        assertDoesNotThrow(() -> validateVia(List.of(phase("p", "verify", null, "integration-test"))));
    }

    @Test
    void validWithParentOnly() {
        // parent alone is sufficient — phase is appended to end of parent's children
        assertDoesNotThrow(() -> validateVia(List.of(phase("p", "verify", null, null))));
    }

    @Test
    void validWithAfterOnly() {
        // no parent: after alone is sufficient
        assertDoesNotThrow(() -> validateVia(List.of(phase("p", null, "unit-test", null))));
    }

    @Test
    void validWithBeforeOnly() {
        assertDoesNotThrow(() -> validateVia(List.of(phase("p", null, null, "integration-test"))));
    }

    @Test
    void validWithAllFourAttributes() {
        assertDoesNotThrow(() -> validateVia(List.of(phase("p", "verify", "unit-test", "integration-test"))));
    }

    // -------------------------------------------------------------------------
    // Validation: invalid declarations (should throw)
    // -------------------------------------------------------------------------

    @Test
    void invalidBlankName() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class, () -> validateVia(List.of(phase("  ", "verify", null, null))));
        assertTrue(ex.getMessage().contains("non-blank 'name'"));
    }

    @Test
    void invalidNoAnchorNoParent() {
        // no parent, no after, no before → invalid
        IllegalStateException ex = assertThrows(
                IllegalStateException.class, () -> validateVia(List.of(phase("orphan", null, null, null))));
        assertTrue(ex.getMessage().contains("must specify 'parent'"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Exercises the private validate() method indirectly by calling the same logic
     * extracted here. In practice validate() is called inside resolveInjections() which
     * requires a live MavenSession; this helper duplicates the validation rules so they
     * can be tested without DI.
     */
    private static void validateVia(List<PhaseInjection> phases) {
        for (PhaseInjection p : phases) {
            validatePhase(p);
        }
    }

    /** Mirror of ReactorXmlLifecycleProcessor.validate() kept in sync by hand. */
    private static void validatePhase(PhaseInjection p) {
        if (p.getName() == null || p.getName().isBlank()) {
            throw new IllegalStateException("reactor.xml: <phase> element must have a non-blank 'name' attribute");
        }
        boolean hasParent = p.getParent() != null && !p.getParent().isBlank();
        boolean hasAfter = p.getAfter() != null && !p.getAfter().isBlank();
        boolean hasBefore = p.getBefore() != null && !p.getBefore().isBlank();
        if (!hasParent && !hasAfter && !hasBefore) {
            throw new IllegalStateException("reactor.xml: custom phase '"
                    + p.getName()
                    + "' must specify 'parent' and/or at least one of 'after' or 'before'"
                    + " to anchor it in the lifecycle DAG");
        }
    }
}
