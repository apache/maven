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

    WrapperProperties(Supplier<Map<String, String>> getter, Consumer<Properties> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public String getProperty(String key) {
        return getter.get().get(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return getter.get().getOrDefault(key, defaultValue);
    }

    @Override
    public Enumeration<?> propertyNames() {
        return Collections.enumeration(getter.get().keySet());
    }

    @Override
    public Set<String> stringPropertyNames() {
        return getter.get().keySet();
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
        return getter.get().size();
    }

    @Override
    public boolean isEmpty() {
        return getter.get().isEmpty();
    }

    @Override
    public Enumeration<Object> keys() {
        return Collections.enumeration((Set) getter.get().keySet());
    }

    @Override
    public Enumeration<Object> elements() {
        return Collections.enumeration((Collection) getter.get().values());
    }

    @Override
    public boolean contains(Object value) {
        return getter.get().containsKey(value != null ? value.toString() : null);
    }

    @Override
    public boolean containsValue(Object value) {
        return getter.get().containsValue(value);
    }

    @Override
    public boolean containsKey(Object key) {
        return getter.get().containsKey(key);
    }

    @Override
    public Object get(Object key) {
        return getter.get().get(key);
    }

    @Override
    public synchronized String toString() {
        return getter.get().toString();
    }

    @Override
    public Set<Object> keySet() {
        return new OrderedProperties(getter.get()).keySet();
    }

    @Override
    public Collection<Object> values() {
        return new OrderedProperties(getter.get()).values();
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return new OrderedProperties(getter.get()).entrySet();
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (o instanceof WrapperProperties wrapperProperties) {
            o = wrapperProperties.getter.get();
        }
        return getter.get().equals(o);
    }

    @Override
    public synchronized int hashCode() {
        return getter.get().hashCode();
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        if (key instanceof String str && defaultValue instanceof String def) {
            return getter.get().getOrDefault(key, def);
        } else {
            return defaultValue;
        }
    }

    @Override
    public synchronized void forEach(BiConsumer<? super Object, ? super Object> action) {
        getter.get().forEach(action);
    }

    interface WriteOp<T> {
        T perform(Properties props);
    }

    interface WriteOpVoid {
        void perform(Properties props);
    }

    private <T> T writeOperation(WriteOp<T> runner) {
        OrderedProperties props = new OrderedProperties(getter.get());
        T ret = runner.perform(props);
        if (!props.equals(getter.get())) {
            setter.accept(props);
        }
        return ret;
    }

    private void writeOperationVoid(WriteOpVoid runner) {
        writeOperation(properties -> {
            runner.perform(properties);
           return null;
        });
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
        new OrderedProperties(getter.get()).save(out, comments);
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        new OrderedProperties(getter.get()).store(writer, comments);
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        new OrderedProperties(getter.get()).store(out, comments);
    }

    @Override
    public synchronized void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeToXML(OutputStream os, String comment) throws IOException {
        new OrderedProperties(getter.get()).storeToXML(os, comment);
    }

    @Override
    public void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
        new OrderedProperties(getter.get()).storeToXML(os, comment, encoding);
    }


    private Object writeReplace() throws java.io.ObjectStreamException {
        return new OrderedProperties(getter.get());
    }

    private class OrderedProperties extends Properties {
        private final List<Object> keyOrder = new CopyOnWriteArrayList<>();

        OrderedProperties(Map<?, ?> map) {
            putAll(map);
        }

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
