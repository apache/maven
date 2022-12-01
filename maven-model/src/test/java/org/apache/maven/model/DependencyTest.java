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

import junit.framework.TestCase;

/**
 * Tests {@code Dependency}.
 *
 * @author Benjamin Bentmann
 */
public class DependencyTest extends TestCase {

    public void testHashCodeNullSafe() {
        new Dependency().hashCode();
    }

    public void testEqualsNullSafe() {
        assertFalse(new Dependency().equals(null));

        new Dependency().equals(new Dependency());
    }

    public void testEqualsIdentity() {
        Dependency thing = new Dependency();
        assertTrue(thing.equals(thing));
    }

    public void testToStringNullSafe() {
        assertNotNull(new Dependency().toString());
    }
}
