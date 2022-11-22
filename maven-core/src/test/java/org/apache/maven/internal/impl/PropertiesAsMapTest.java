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
package org.apache.maven.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class PropertiesAsMapTest {

    @Test
    public void testPropertiesAsMap() {
        Properties props = new Properties();
        props.setProperty("foo1", "bar1");
        props.setProperty("foo2", "bar2");
        PropertiesAsMap pam = new PropertiesAsMap(props);
        assertEquals(2, pam.size());
        Set<Entry<String, String>> set = pam.entrySet();
        assertNotNull(set);
        assertEquals(2, set.size());
        Iterator<Entry<String, String>> iterator = set.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        Entry<String, String> entry = iterator.next();
        assertNotNull(entry);
        entry = iterator.next();
        assertNotNull(entry);
        assertThrows(NoSuchElementException.class, () -> iterator.next());
        assertFalse(iterator.hasNext());
    }
}
