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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

class PropertiesAsMap extends AbstractMap<String, String> {

    private final Map<Object, Object> properties;

    PropertiesAsMap(Map<Object, Object> properties) {
        this.properties = properties;
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return new AbstractSet<Entry<String, String>>() {
            @Override
            public Iterator<Entry<String, String>> iterator() {
                Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator();
                return new Iterator<Entry<String, String>>() {
                    Entry<String, String> next;

                    {
                        advance();
                    }

                    private void advance() {
                        next = null;
                        while (iterator.hasNext()) {
                            Entry<Object, Object> e = iterator.next();
                            if (PropertiesAsMap.matches(e)) {
                                next = new Entry<String, String>() {
                                    @Override
                                    public String getKey() {
                                        return (String) e.getKey();
                                    }

                                    @Override
                                    public String getValue() {
                                        return (String) e.getValue();
                                    }

                                    @Override
                                    public String setValue(String value) {
                                        return (String) e.setValue(value);
                                    }
                                };
                                break;
                            }
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        return next != null;
                    }

                    @Override
                    public Entry<String, String> next() {
                        Entry<String, String> item = next;
                        if (item == null) {
                            throw new NoSuchElementException();
                        }
                        advance();
                        return item;
                    }
                };
            }

            @Override
            public int size() {
                return (int) properties.entrySet().stream()
                        .filter(PropertiesAsMap::matches)
                        .count();
            }
        };
    }

    private static boolean matches(Entry<Object, Object> entry) {
        return entry.getKey() instanceof String && entry.getValue() instanceof String;
    }
}
