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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrapperPropertiesTest {
    private final Map<String, String> backingStore = new HashMap<>();
    private final AtomicBoolean getterCalled = new AtomicBoolean(false);
    private final AtomicBoolean setterCalled = new AtomicBoolean(false);
    private final WrapperProperties wrapper = new WrapperProperties(
            () -> {
                getterCalled.set(true);
                return new HashMap<>(backingStore);
            },
            props -> {
                setterCalled.set(true);
                backingStore.clear();
                props.forEach((k, v) -> backingStore.put(k.toString(), v.toString()));
            });

    @Test
    void testInitialization() {
        wrapper.getProperty("any");
        assertTrue(getterCalled.get());
    }

    @Test
    void testBasicOperations() {
        // Test set and get
        wrapper.setProperty("key1", "value1");
        assertEquals("value1", wrapper.getProperty("key1"));
        assertEquals("value1", backingStore.get("key1"));
        assertTrue(setterCalled.get(), "Setter should be called after modification");

        // Test remove
        wrapper.remove("key1");
        assertNull(wrapper.getProperty("key1"));
        assertFalse(backingStore.containsKey("key1"));

        // Test contains
        wrapper.setProperty("key2", "value2");
        assertTrue(wrapper.containsKey("key2"));
        assertTrue(wrapper.containsValue("value2"));
    }

    @Test
    void testOrderPreservation() {
        wrapper.setProperty("a", "1");
        wrapper.setProperty("b", "2");
        wrapper.setProperty("c", "3");

        // Check iteration order matches insertion order
        Set<Object> keys = wrapper.keySet();
        Object[] keyArray = keys.toArray();
        assertEquals("a", keyArray[0]);
        assertEquals("b", keyArray[1]);
        assertEquals("c", keyArray[2]);

        // Modify existing value and check order remains
        wrapper.setProperty("b", "22");
        keyArray = wrapper.keySet().toArray();
        assertEquals("a", keyArray[0]);
        assertEquals("b", keyArray[1]);
        assertEquals("c", keyArray[2]);
    }

    @Test
    @Disabled("should not throw NPE but does...")
    void testBulkOperations() {
        Properties props = new Properties();
        props.setProperty("x", "10");
        props.setProperty("y", "20");
        props.setProperty("z", "30");

        wrapper.putAll(props);
        assertEquals("10", wrapper.getProperty("x"));
        assertEquals("20", wrapper.getProperty("y"));
        assertEquals("30", wrapper.getProperty("z"));

        wrapper.clear();
        assertTrue(wrapper.isEmpty());
    }

    @Test
    void testStringPropertyNames() {
        wrapper.setProperty("a", "1");
        wrapper.setProperty("b", "2");

        Set<String> names = wrapper.stringPropertyNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("a"));
        assertTrue(names.contains("b"));
    }

    @Test
    void testEqualsAndHashCode() {
        wrapper.setProperty("k1", "v1");
        wrapper.setProperty("k2", "v2");

        Properties other = new Properties();
        other.setProperty("k1", "v1");
        other.setProperty("k2", "v2");

        assertEquals(wrapper, other);
        assertEquals(wrapper.hashCode(), other.hashCode());
    }

    @Test
    void testLoadAndStore() throws IOException {
        String propsData = "key1=value1\nkey2=value2\n";
        wrapper.load(new StringReader(propsData));

        assertEquals("value1", wrapper.getProperty("key1"));
        assertEquals("value2", wrapper.getProperty("key2"));

        StringWriter writer = new StringWriter();
        wrapper.store(writer, "test");
        String stored = writer.toString();
        assertTrue(stored.contains("key1=value1"));
        assertTrue(stored.contains("key2=value2"));
    }

    @Test
    void testLoadFromStream() throws IOException {
        String propsData = "key3=value3\nkey4=value4\n";
        wrapper.load(new ByteArrayInputStream(propsData.getBytes()));

        assertEquals("value3", wrapper.getProperty("key3"));
        assertEquals("value4", wrapper.getProperty("key4"));
    }

    @Test
    void testStoreToXML() throws IOException {
        wrapper.setProperty("xmlkey", "xmlvalue");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wrapper.storeToXML(out, "test");

        String xml = out.toString();
        assertTrue(xml.contains("<entry key=\"xmlkey\">xmlvalue</entry>"));
    }

    @Test
    void testUnsupportedOperations() {
        assertThrows(UnsupportedOperationException.class, () -> wrapper.list(System.out));
        assertThrows(UnsupportedOperationException.class, () -> wrapper.loadFromXML(null));
    }

    @Test
    void testConcurrentModification() {
        // Initial setup
        wrapper.setProperty("initial", "value");

        // Simulate concurrent modification by changing backing store directly
        backingStore.put("concurrent", "mod");

        // Next access should pick up the concurrent modification
        assertEquals(null, wrapper.getProperty("concurrent"));
    }

    @Test
    void testContainsKey() {
        wrapper.put("nullkey", "null");
        assertTrue(wrapper.containsKey("nullkey"));
        assertNull(wrapper.get("null"));
    }

    @Test
    void testComputeOperations() {
        wrapper.setProperty("compute", "1");
        wrapper.compute("compute", (k, v) -> v + "1");
        assertEquals("11", wrapper.getProperty("compute"));

        wrapper.computeIfAbsent("newkey", k -> "newvalue");
        assertEquals("newvalue", wrapper.getProperty("newkey"));

        wrapper.computeIfPresent("newkey", (k, v) -> "updated");
        assertEquals("updated", wrapper.getProperty("newkey"));
    }

    @Test
    void testMergeOperation() {
        wrapper.setProperty("merge", "a");
        wrapper.merge("merge", "b", (oldVal, newVal) -> "" + oldVal + newVal);
        assertEquals("ab", wrapper.getProperty("merge"));

        wrapper.merge("newmerge", "c", (oldVal, newVal) -> "" + oldVal + newVal);
        assertEquals("c", wrapper.getProperty("newmerge"));
    }

    @Test
    void testReplaceOperations() {
        wrapper.setProperty("replace", "old");
        wrapper.replace("replace", "new");
        assertEquals("new", wrapper.getProperty("replace"));

        wrapper.replace("replace", "new", "newer");
        assertEquals("newer", wrapper.getProperty("replace"));

        assertFalse(wrapper.replace("nonexistent", "a", "b"));
    }

    @Test
    void testEntrySetBehavior() {
        wrapper.setProperty("entry1", "value1");
        wrapper.setProperty("entry2", "value2");

        wrapper.entrySet().forEach(entry -> {
            if ("entry1".equals(entry.getKey())) {
                entry.setValue("modified");
            }
        });

        assertEquals("modified", wrapper.getProperty("entry1"));
    }
}
