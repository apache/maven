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
package org.apache.maven.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@code Scm}.
 *
 */
class ScmTest {

    @Test
    void testHashCodeNullSafe() {
        new Scm().hashCode();
    }

    @Test
    void testEqualsNullSafe() {
        assertFalse(new Scm().equals(null));

        new Scm().equals(new Scm());
    }

    @Test
    void testEqualsIdentity() {
        Scm thing = new Scm();
        assertTrue(thing.equals(thing));
    }

    @Test
    void testToStringNullSafe() {
        assertNotNull(new Scm().toString());
    }

    public void testToStringNotNonsense() {
        Scm scm = new Scm();
        scm.setConnection("scm:git:git://git.localdomain/model");

        String s = scm.toString();

        assertEquals("Scm {connection=scm:git:git://git.localdomain/model}", s);
    }
}
