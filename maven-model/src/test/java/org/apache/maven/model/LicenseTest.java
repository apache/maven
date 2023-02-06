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
 * Tests {@code License}.
 *
 * @author Benjamin Bentmann
 */
public class LicenseTest extends TestCase {

    public void testHashCodeNullSafe() {
        new License().hashCode();
    }

    public void testEqualsNullSafe() {
        assertFalse(new License().equals(null));

        new License().equals(new License());
    }

    public void testEqualsIdentity() {
        License thing = new License();
        assertTrue(thing.equals(thing));
    }

    public void testToStringNullSafe() {
        assertNotNull(new License().toString());
    }

    public void testToStringNotNonsense() {
        License license = new License();
        license.setName("Unlicense");
        license.setUrl("http://lic.localdomain");

        String s = license.toString();

        assert "License {name=Unlicense, url=http://lic.localdomain}".equals(s) : s;
    }
}
