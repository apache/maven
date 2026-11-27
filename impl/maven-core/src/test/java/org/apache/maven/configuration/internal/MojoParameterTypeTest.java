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
package org.apache.maven.configuration.internal;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MojoParameterTypeTest {

    @Test
    void acceptsSafeJdkTypes() {
        assertSafe(String.class);
        assertSafe(Integer.class);
        assertSafe(boolean.class);
        assertSafe(File.class);
        assertSafe(Path.class);
        assertSafe(URI.class);
    }

    @Test
    void acceptsSafeParameterizedCollections() throws Exception {
        assertSafe(fieldType("arrayList"));
        assertSafe(fieldType("hashMap"));
        assertSafe(fieldType("strings"));
        assertSafe(fieldType("values"));
    }

    @Test
    void rejectsUnsafeTypes() throws Exception {
        assertUnsafe(PluginValue.class);
        assertUnsafe(fieldType("pluginList"));
        assertUnsafe(fieldType("rawList"));
        assertUnsafe(fieldType("unknownValues"));
        assertUnsafe(fieldType("pluginValues"));
    }

    private static void assertSafe(Type type) {
        assertTrue(MojoParameterType.isSafe(type), () -> "Expected safe type: " + type);
    }

    private static void assertUnsafe(Type type) {
        assertFalse(MojoParameterType.isSafe(type), () -> "Expected unsafe type: " + type);
    }

    private static Type fieldType(String name) throws Exception {
        Field field = TypeSamples.class.getDeclaredField(name);
        return field.getGenericType();
    }

    private static class PluginList<T> extends ArrayList<T> {}

    private static class PluginValue {}

    private static class TypeSamples {
        PluginList<String> pluginList;
        ArrayList<String> arrayList;
        HashMap<String, Integer> hashMap;
        List<String> strings;
        Map<String, Integer> values;
        List rawList;
        List<?> unknownValues;
        List<PluginValue> pluginValues;
    }
}
