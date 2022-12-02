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
 * Tests {@code Prerequisites}.
 *
 * @author Benjamin Bentmann
 */
public class PrerequisitesTest extends TestCase {

    public void testHashCodeNullSafe() {
        new Prerequisites().hashCode();
    }

    public void testEqualsNullSafe() {
        assertFalse(new Prerequisites().equals(null));

        new Prerequisites().equals(new Prerequisites());
    }

    public void testEqualsIdentity() {
        Prerequisites thing = new Prerequisites();
        assertTrue(thing.equals(thing));
    }

    public void testToStringNullSafe() {
        assertNotNull(new Prerequisites().toString());
    }
}
