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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests {@code Organization}.
 *
 * @author Benjamin Bentmann
 */
public class OrganizationTest {

    @Test
    public void testHashCodeNullSafe() {
        new Organization().hashCode();
    }

    @Test
    public void testEqualsNullSafe() {
        assertFalse(new Organization().equals(null));

        new Organization().equals(new Organization());
    }

    @Test
    public void testEqualsIdentity() {
        Organization thing = new Organization();
        assertTrue(thing.equals(thing));
    }

    @Test
    public void testToStringNullSafe() {
        assertNotNull(new Organization().toString());
    }

    public void testToStringNotNonsense11() {
        Organization org = new Organization();
        org.setName("Testing Maven Unit");
        org.setUrl("https://maven.localdomain");

        assertEquals("Organization {name=Testing Maven Unit, url=https://maven.localdomain}", org.toString());
    }

    public void testToStringNotNonsense10() {
        Organization org = new Organization();
        org.setName("Testing Maven Unit");

        assertEquals("Organization {name=Testing Maven Unit, url=null}", org.toString());
    }

    public void testToStringNotNonsense01() {
        Organization org = new Organization();
        org.setUrl("https://maven.localdomain");

        assertEquals("Organization {name=null, url=https://maven.localdomain}", org.toString());
    }

    public void testToStringNotNonsense00() {
        Organization org = new Organization();

        assertEquals("Organization {name=null, url=null}", org.toString());
    }
}
