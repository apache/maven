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
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
                props.forEach((k, v) -> backingStore.put(k.toString(), v != null ? v.toString():null)); // potential NPE
            });

    @Test
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
        assertNull(wrapper.getProperty("x"));
        assertNull(wrapper.getProperty("y"));
        assertNull(wrapper.getProperty("z"));
    }

    @Test
    void testRemoveWithValue() {
        wrapper.setProperty("toremove", "val");
        assertFalse(wrapper.remove("toremove", "wrongval"));
        assertTrue(wrapper.remove("toremove", "val"));
        assertNull(wrapper.getProperty("toremove"));
    }

    @Test
    void testInitialization() {
        wrapper.getProperty("any");
        assertTrue(getterCalled.get());
    }

    @Test
    void testBasicContainsKey() {
        wrapper.setProperty("key1", "value1");
        assertEquals("value1", wrapper.getProperty("key1"));
        assertEquals("value1", backingStore.get("key1"));
        assertTrue(setterCalled.get());

        wrapper.remove("key1");
        assertNull(wrapper.getProperty("key1"));
        assertFalse(backingStore.containsKey("key1"));
    }

    @Test
    void testBasicContainsKeyMultipleTimes() {
        wrapper.setProperty("key1", "value1");
        assertEquals("value1", wrapper.getProperty("key1"));
        assertEquals("value1", backingStore.get("key1"));
        assertTrue(setterCalled.get());

        wrapper.remove("key1");
        wrapper.setProperty("key2", "value2");
        assertTrue(wrapper.containsKey("key2"));
        assertTrue(wrapper.containsValue("value2"));
    }

    @Test
    void testOrderPreservation() {
        wrapper.setProperty("a", "1");
        wrapper.setProperty("b", "2");
        wrapper.setProperty("c", "3");

        Set<Object> keys = wrapper.keySet();
        Object[] keyArray = keys.toArray();
        assertEquals("a", keyArray[0]);
        assertEquals("b", keyArray[1]);
        assertEquals("c", keyArray[2]);

        wrapper.setProperty("b", "22");
        keyArray = wrapper.keySet().toArray();
        assertEquals("a", keyArray[0]);
        assertEquals("b", keyArray[1]);
        assertEquals("c", keyArray[2]);
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
        wrapper.setProperty("initial", "value");
        backingStore.put("concurrent", "mod");
        assertNull(wrapper.getProperty("concurrent"));
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

    @Test
    void testDefaultValueProperty() {
        assertNull(wrapper.getProperty("nonexistent"));
        assertEquals("default", wrapper.getProperty("nonexistent", "default"));
    }

    @Test
    void testElementsEnumeration() {
        wrapper.setProperty("enum1", "val1");
        wrapper.setProperty("enum2", "val2");

        Enumeration<Object> elements = wrapper.elements();
        List<Object> values = Collections.list(elements);
        assertEquals(2, values.size());
        assertTrue(values.contains("val1"));
        assertTrue(values.contains("val2"));
    }

    @Test
    void testKeysEnumeration() {
        wrapper.setProperty("k1", "v1");
        wrapper.setProperty("k2", "v2");

        Enumeration<Object> keys = wrapper.keys();
        List<Object> keyList = Collections.list(keys);
        assertEquals(2, keyList.size());
        assertTrue(keyList.contains("k1"));
        assertTrue(keyList.contains("k2"));
    }

    @Test
    void testPropertyNamesEnumeration() {
        Properties wrapper = new Properties();
        wrapper.setProperty("p1", "pv1");
        wrapper.setProperty("p2", "pv2");

        List<String> names = Collections.list((Enumeration<String>) wrapper.propertyNames());

        assertEquals(2, names.size());
        assertTrue(names.contains("p1"));
        assertTrue(names.contains("p2"));
    }

    @Test
    void testValuesCollection() {
        wrapper.setProperty("vc1", "vcval1");
        wrapper.setProperty("vc2", "vcval2");

        Collection<Object> values = wrapper.values();
        assertEquals(2, values.size());
        assertTrue(values.contains("vcval1"));
        assertTrue(values.contains("vcval2"));
    }

    @Test
    void testPutIfAbsent() {
        assertNull(wrapper.putIfAbsent("absent", "value"));
        assertEquals("value", wrapper.putIfAbsent("absent", "newvalue"));
        assertEquals("value", wrapper.getProperty("absent"));
    }

    @Test
    void testReplaceAll() {
        wrapper.setProperty("ra1", "1");
        wrapper.setProperty("ra2", "2");

        wrapper.replaceAll((k, v) -> v.toString() + v);

        assertEquals("11", wrapper.getProperty("ra1"));
        assertEquals("22", wrapper.getProperty("ra2"));
    }

    @Test
    void testForEach() {
        wrapper.setProperty("fe1", "fv1");
        wrapper.setProperty("fe2", "fv2");

        Map<String, String> collected = new HashMap<>();
        wrapper.forEach((k, v) -> collected.put(k.toString(), v.toString()));

        assertEquals(2, collected.size());
        assertEquals("fv1", collected.get("fe1"));
        assertEquals("fv2", collected.get("fe2"));
    }

    @Test
    void testGetOrDefault() {
        wrapper.setProperty("gd1", "gv1");
        assertEquals("gv1", wrapper.getOrDefault("gd1", "default"));
        assertEquals("default", wrapper.getOrDefault("nonexistent", "default"));
    }

    @Test
    void testSaveMethod() throws IOException {
        wrapper.setProperty("save1", "sval1");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wrapper.save(out, "comments");

        String saved = out.toString();
        assertTrue(saved.contains("save1=sval1"));
    }

    @Test
    void testStoreWithComments() throws IOException {
        wrapper.setProperty("store1", "sval1");
        StringWriter writer = new StringWriter();
        wrapper.store(writer, "test comments");

        String stored = writer.toString();
        assertTrue(stored.contains("store1=sval1"));
        assertTrue(stored.contains("#test comments"));
    }

    @Test
    void testStoreToXMLWithEncoding() throws IOException {
        wrapper.setProperty("xmlenc", "xmlvalue");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wrapper.storeToXML(out, "test", "UTF-8");

        String xml = out.toString("UTF-8");
        assertTrue(xml.contains("<entry key=\"xmlenc\">xmlvalue</entry>"));
    }

    @Test
    void testEqualsWithDifferentProperties() {
        wrapper.setProperty("eq1", "val1");

        Properties other = new Properties();
        other.setProperty("eq2", "val2");

        assertNotEquals(wrapper, other);
    }

    @Test
    void testHashCodeConsistency() {
        wrapper.setProperty("hc1", "val1");
        int initialHash = wrapper.hashCode();

        wrapper.setProperty("hc2", "val2");
        assertNotEquals(initialHash, wrapper.hashCode());
    }

    @Test
    void testToStringContainsProperties() {
        wrapper.setProperty("ts1", "tval1");
        String str = wrapper.toString();

        assertTrue(str.contains("ts1=tval1"));
    }

    @Test
    void testConcurrentInitialization() throws InterruptedException {
        final AtomicBoolean initialized = new AtomicBoolean(false);

        Thread t1 = new Thread(() -> {
            wrapper.getProperty("any");
            initialized.set(true);
        });

        Thread t2 = new Thread(() -> {
            while (!initialized.get()) {
                // wait
            }
            wrapper.setProperty("concurrent", "value");
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals("value", wrapper.getProperty("concurrent"));
    }
}
