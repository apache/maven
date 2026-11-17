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
package org.apache.maven.di.impl;

import java.lang.ref.Reference;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DiagnosticIdentityTest {
    @Test
    void collidingLoadersReceiveDistinctStableLabels() {
        DiagnosticIdentity identities = new DiagnosticIdentity(object -> 1);
        ClassLoader first = new ClassLoader() {};
        ClassLoader second = new ClassLoader() {};
        assertEquals("L1", identities.loader(first));
        assertEquals("L2", identities.loader(second));
        assertEquals("L1", identities.loader(first));
        assertEquals("L2", identities.loader(second));
        assertEquals(2, identities.loaders().size());
        assertEquals("bootstrap", identities.loader(null));
    }

    @Test
    void collidingWeakReferencesCompareReferentsAndCleanUpIndividually() throws Exception {
        DiagnosticIdentity identities = new DiagnosticIdentity(object -> 1);
        Object first = new Object();
        Object second = new Object();
        long firstToken = identities.token(first);
        long secondToken = identities.token(second);
        assertNotEquals(firstToken, secondToken);
        assertEquals(firstToken, identities.token(first));
        var field = DiagnosticIdentity.class.getDeclaredField("tokens");
        field.setAccessible(true);
        Map<?, ?> tokens = (Map<?, ?>) field.get(identities);
        // Simulate collection of only one entry; the colliding live entry must keep its token.
        Reference<?> collected = tokens.keySet().stream()
                .map(key -> (Reference<?>) key)
                .filter(reference -> reference.get() == first)
                .findFirst()
                .orElseThrow();
        collected.clear();
        collected.enqueue();
        assertEquals(secondToken, identities.token(second));
        assertEquals(1, tokens.size());
        long replacementToken = identities.token(first);
        assertNotEquals(firstToken, replacementToken);
        assertNotEquals(secondToken, replacementToken);
        assertEquals(2, tokens.size());
    }
}
