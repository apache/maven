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

}
