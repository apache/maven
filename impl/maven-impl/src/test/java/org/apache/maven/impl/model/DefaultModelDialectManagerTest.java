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
package org.apache.maven.impl.model;

import java.util.Collections;

import org.apache.maven.api.Dialect;
import org.apache.maven.impl.DefaultModelXmlFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultModelDialectManagerTest {

    @Test
    void xmlDialectIsAlwaysAvailable() {
        DefaultModelDialectManager manager =
                new DefaultModelDialectManager(new DefaultModelXmlFactory(), Collections.emptyList());
        assertEquals(1, manager.getAvailableDialects().size());
        assertTrue(manager.getAvailableDialects().contains(Dialect.XML));
    }

    @Test
    void unknownDialectThrowsIllegalArgumentException() {
        DefaultModelDialectManager manager =
                new DefaultModelDialectManager(new DefaultModelXmlFactory(), Collections.emptyList());
        // Dialect.of() creates a proper Dialect instance with correct equals/hashCode.
        // Since "toml" is not registered, the manager must throw.
        Dialect toml = Dialect.of("toml");
        assertThrows(IllegalArgumentException.class, () -> manager.readModel(null, toml, null));
    }

    // -------------------------------------------------------------------------
    // Dialect.of() contract tests
    // -------------------------------------------------------------------------

    @Test
    void dialectOfReturnsNonNullWithCorrectId() {
        Dialect yaml = Dialect.of("yaml");
        assertNotNull(yaml);
        assertEquals("yaml", yaml.id());
    }

    @Test
    void dialectOfEqualsWhenSameId() {
        Dialect a = Dialect.of("yaml");
        Dialect b = Dialect.of("yaml");
        assertEquals(a, b, "Two Dialect.of() with same id must be equal");
        assertEquals(a.hashCode(), b.hashCode(), "Equal dialects must have equal hashCodes");
    }

    @Test
    void dialectOfNotEqualsWhenDifferentId() {
        Dialect yaml = Dialect.of("yaml");
        Dialect toml = Dialect.of("toml");
        assertNotEquals(yaml, toml);
    }

    @Test
    void dialectOfXmlEqualsDialectXmlConstant() {
        // Dialect.of("xml") must equal Dialect.XML
        Dialect xml = Dialect.of("xml");
        assertEquals(Dialect.XML, xml, "Dialect.of(\"xml\") must equal Dialect.XML");
        assertEquals(Dialect.XML.hashCode(), xml.hashCode());
    }

    @Test
    void dialectOfCanBeUsedAsMapKey() {
        // Verifies that equals+hashCode are correct for map usage
        java.util.Map<Dialect, String> map = new java.util.HashMap<>();
        Dialect yaml1 = Dialect.of("yaml");
        Dialect yaml2 = Dialect.of("yaml");
        map.put(yaml1, "value");
        assertEquals("value", map.get(yaml2), "Dialect.of() instances with same id must map to the same key");
    }
}
