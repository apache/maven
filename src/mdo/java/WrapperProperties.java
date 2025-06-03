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
package ${package};

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class WrapperProperties extends Properties {

    final Supplier<Map<String, String>> getter;
    final Consumer<Properties> setter;
    private final OrderedProperties orderedProps = new OrderedProperties();
    private boolean initialized;

    WrapperProperties(Supplier<Map<String, String>> getter, Consumer<Properties> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    private synchronized void ensureInitialized() {
        if (!initialized) {
            orderedProps.putAll(getter.get());
            initialized = true;
        }
    }

    @Override
    public String getProperty(String key) {
        ensureInitialized();
        return orderedProps.getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        ensureInitialized();
        return orderedProps.getProperty(key, defaultValue);
    }

    @Override
    public Enumeration<?> propertyNames() {
        ensureInitialized();
        return orderedProps.propertyNames();
    }

    @Override
    public Set<String> stringPropertyNames() {
        ensureInitialized();
        return orderedProps.stringPropertyNames();
    }

    @Override
    public void list(PrintStream out) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void list(PrintWriter out) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        ensureInitialized();
        return orderedProps.size();
    }

    @Override
    public boolean isEmpty() {
        ensureInitialized();
        return orderedProps.isEmpty();
    }

    @Override
    public Enumeration<Object> keys() {
        ensureInitialized();
        return orderedProps.keys();
    }

    @Override
    public Enumeration<Object> elements() {
        ensureInitialized();
        return orderedProps.elements();
    }

    @Override
    public boolean contains(Object value) {
        ensureInitialized();
        return orderedProps.contains(value);
    }

    @Override
    public boolean containsValue(Object value) {
        ensureInitialized();
        return orderedProps.containsValue(value);
    }

    @Override
    public boolean containsKey(Object key) {
        ensureInitialized();
        return orderedProps.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        ensureInitialized();
        return orderedProps.get(key);
    }

    @Override
    public synchronized String toString() {
        ensureInitialized();
        return orderedProps.toString();
    }

    @Override
    public Set<Object> keySet() {
        ensureInitialized();
        return orderedProps.keySet();
    }

    @Override
    public Collection<Object> values() {
        ensureInitialized();
        return orderedProps.values();
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        ensureInitialized();
        return orderedProps.entrySet();
    }

    @Override
    public synchronized boolean equals(Object o) {
        ensureInitialized();
        if (o instanceof WrapperProperties wrapperProperties) {
            wrapperProperties.ensureInitialized();
            return orderedProps.equals(wrapperProperties.orderedProps);
        }
        return orderedProps.equals(o);
    }

    @Override
    public synchronized int hashCode() {
        ensureInitialized();
        return orderedProps.hashCode();
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        ensureInitialized();
        return orderedProps.getOrDefault(key, defaultValue);
    }

    @Override
    public synchronized void forEach(BiConsumer<? super Object, ? super Object> action) {
        ensureInitialized();
        orderedProps.forEach(action);
    }

    interface WriteOp<T> {
        T perform(Properties props);
    }

    interface WriteOpVoid {
        void perform(Properties props);
    }

    private <T> T writeOperation(WriteOp<T> runner) {
        ensureInitialized();
        T ret = runner.perform(orderedProps);
        setter.accept(orderedProps);
        return ret;
    }

    private void writeOperationVoid(WriteOpVoid runner) {
        ensureInitialized();
        runner.perform(orderedProps);
        setter.accept(orderedProps);
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        return writeOperation(p -> p.setProperty(key, value));
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        return writeOperation(p -> p.put(key, value));
    }

    @Override
    public synchronized Object remove(Object key) {
        return writeOperation(p -> p.remove(key));
    }

    @Override
    public synchronized void putAll(Map<?, ?> t) {
        writeOperationVoid(p -> p.putAll(t));
    }

    @Override
    public synchronized void clear() {
        writeOperationVoid(Properties::clear);
    }

    @Override
    public synchronized void replaceAll(BiFunction<? super Object, ? super Object, ?> function) {
        writeOperationVoid(p -> p.replaceAll(function));
    }

    @Override
    public synchronized Object putIfAbsent(Object key, Object value) {
        return writeOperation(p -> p.putIfAbsent(key, value));
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        return writeOperation(p -> p.remove(key, value));
    }

    @Override
    public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
        return writeOperation(p -> p.replace(key, oldValue, newValue));
    }

    @Override
    public synchronized Object replace(Object key, Object value) {
        return writeOperation(p -> p.replace(key, value));
    }

    @Override
    public synchronized Object computeIfAbsent(Object key, Function<? super Object, ?> mappingFunction) {
        return writeOperation(p -> p.computeIfAbsent(key, mappingFunction));
    }

    @Override
    public synchronized Object computeIfPresent(
            Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        return writeOperation(p -> p.computeIfPresent(key, remappingFunction));
    }

    @Override
    public synchronized Object compute(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        return writeOperation(p -> p.compute(key, remappingFunction));
    }

    @Override
    public synchronized Object merge(
            Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        return writeOperation(p -> p.merge(key, value, remappingFunction));
    }

    @Override
    public synchronized void load(Reader reader) throws IOException {
        try {
            writeOperationVoid(p -> {
                try {
                    p.load(reader);
                } catch (IOException e) {
                    throw new IOError(e);
                }
            });
        } catch (IOError e) {
            throw (IOException) e.getCause();
        }
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        try {
            writeOperationVoid(p -> {
                try {
                    p.load(inStream);
                } catch (IOException e) {
                    throw new IOError(e);
                }
            });
        } catch (IOError e) {
            throw (IOException) e.getCause();
        }
    }

    @Override
    public void save(OutputStream out, String comments) {
        Properties props = new Properties();
        props.putAll(getter.get());
        props.save(out, comments);
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        Properties props = new Properties();
        props.putAll(getter.get());
        props.store(writer, comments);
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        Properties props = new Properties();
        props.putAll(getter.get());
        props.store(out, comments);
    }

    @Override
    public synchronized void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeToXML(OutputStream os, String comment) throws IOException {
        Properties props = new Properties();
        props.putAll(getter.get());
        props.storeToXML(os, comment);
    }

    @Override
    public void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
        Properties props = new Properties();
        props.putAll(getter.get());
        props.storeToXML(os, comment, encoding);
    }


    private Object writeReplace() throws java.io.ObjectStreamException {
        Properties props = new Properties();
        props.putAll(getter.get());
        return props;
    }

    private class OrderedProperties extends Properties {
        private final List<Object> keyOrder = new CopyOnWriteArrayList<>();

        @Override
        public synchronized void putAll(Map<?, ?> t) {
            t.forEach(this::put);
        }

        @Override
        public Set<Object> keySet() {
            return new KeySet();
        }

        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
            return new EntrySet();
        }

        @Override
        public synchronized Object put(Object key, Object value) {
            if (!keyOrder.contains(key)) {
                keyOrder.add(key);
            }
            return super.put(key, value);
        }

        @Override
        public synchronized Object setProperty(String key, String value) {
            if (!keyOrder.contains(key)) {
                keyOrder.add(key);
            }
            return super.setProperty(key, value);
        }

        @Override
        public synchronized Object remove(Object key) {
            keyOrder.remove(key);
            return super.remove(key);
        }

        @Override
        public synchronized void forEach(BiConsumer<? super Object, ? super Object> action) {
            entrySet().forEach(e -> action.accept(e.getKey(), e.getValue()));
        }

        private class EntrySet extends AbstractSet<Map.Entry<Object, Object>> {
            @Override
            public Iterator<Map.Entry<Object, Object>> iterator() {
                return new Iterator<Map.Entry<Object, Object>>() {
                    Iterator<Object> keyIterator = keyOrder.iterator();
                    @Override
                    public boolean hasNext() {
                        return keyIterator.hasNext();
                    }

                    @Override
                    public Map.Entry<Object, Object> next() {
                        Object key = keyIterator.next();
                        return new Map.Entry<>() {
                            @Override
                            public Object getKey() {
                                return key;
                            }

                            @Override
                            public Object getValue() {
                                return get(key);
                            }

                            @Override
                            public Object setValue(Object value) {
                                return WrapperProperties.this.put(key, value);
                            }
                        };
                    }
                };
            }

            @Override
            public int size() {
                return keyOrder.size();
            }
        }

        private class KeySet extends AbstractSet<Object> {
            public Iterator<Object> iterator() {
                final Iterator<Object> iter = keyOrder.iterator();
                return new Iterator<Object>() {
                    Object lastRet = null;
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public Object next() {
                        lastRet = iter.next();
                        return lastRet;
                    }

                    @Override
                    public void remove() {
                        WrapperProperties.super.remove(lastRet);
                    }
                };
            }

            public int size() {
                return keyOrder.size();
            }

            public boolean contains(Object o) {
                return containsKey(o);
            }

            public boolean remove(Object o) {
                boolean b = WrapperProperties.this.containsKey(o);
                WrapperProperties.this.remove(o);
                return b;
            }

            public void clear() {
                WrapperProperties.this.clear();
            }
        }
    }
}
