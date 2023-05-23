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
package org.apache.maven.internal.build.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.maven.api.build.spi.Message;

public class DefaultBuildContextState implements Serializable {

    private static final long serialVersionUID = 6195150574931820441L;

    final Map<String, Serializable> configuration;

    private final Set<Path> outputs;

    private final Map<Path, FileState> resources;

    private final Map<Path, Collection<Path>> resourceOutputs;

    // pure in-memory performance optimization, always reflects contents of resourceOutputs
    private final Map<Path, Collection<Path>> outputInputs;

    private final Map<Path, Map<String, Serializable>> resourceAttributes;

    private final Map<Path, Collection<Message>> resourceMessages;

    private DefaultBuildContextState(
            Map<String, Serializable> configuration,
            Map<Path, FileState> inputs,
            Set<Path> outputs,
            Map<Path, Collection<Path>> resourceOutputs,
            Map<Path, Collection<Path>> outputInputs,
            Map<Path, Map<String, Serializable>> resourceAttributes,
            Map<Path, Collection<Message>> resourceMessages) {
        this.configuration = configuration;
        this.resources = inputs;
        this.outputs = outputs;
        this.resourceOutputs = resourceOutputs;
        this.outputInputs = outputInputs;
        this.resourceAttributes = resourceAttributes;
        this.resourceMessages = resourceMessages;
    }

    public static DefaultBuildContextState withConfiguration(Map<String, Serializable> configuration) {
        HashMap<String, Serializable> copy = new HashMap<>(configuration);
        // configuration marker used to distinguish between empty and new state
        copy.put("incremental", Boolean.TRUE);
        return new DefaultBuildContextState(
                Collections.unmodifiableMap(copy), // configuration
                new HashMap<>(), // inputs
                new HashSet<>(), // outputs
                new HashMap<>(), // inputOutputs
                new HashMap<>(), // outputInputs
                new HashMap<>(), // resourceAttributes
                new HashMap<>() // messages
                );
    }

    public static DefaultBuildContextState emptyState() {
        return new DefaultBuildContextState(
                Collections.emptyMap(), // configuration
                Collections.emptyMap(), // inputs
                Collections.emptySet(), // outputs
                Collections.emptyMap(), // inputOutputs
                Collections.emptyMap(), // outputInputs
                Collections.emptyMap(), // resourceAttributes
                Collections.emptyMap() // messages
                );
    }

    private static void writeMap(ObjectOutputStream oos, Map<?, ?> map) throws IOException {
        oos.writeInt(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            oos.writeObject(entry.getKey());
            oos.writeObject(entry.getValue());
        }
    }

    private static void writeMultimap(ObjectOutputStream oos, Map<?, ? extends Collection<?>> mmap) throws IOException {
        oos.writeInt(mmap.size());
        for (Map.Entry<?, ? extends Collection<?>> entry : mmap.entrySet()) {
            oos.writeObject(entry.getKey());
            writeCollection(oos, entry.getValue());
        }
    }

    private static void writeCollection(ObjectOutputStream oos, Collection<?> collection) throws IOException {
        if (collection == null || collection.isEmpty()) {
            oos.writeInt(0);
        } else {
            oos.writeInt(collection.size());
            for (Object element : collection) {
                oos.writeObject(element);
            }
        }
    }

    private static void writeDoublemap(ObjectOutputStream oos, Map<?, ? extends Map<?, ?>> dmap) throws IOException {
        oos.writeInt(dmap.size());
        for (Map.Entry<?, ? extends Map<?, ?>> entry : dmap.entrySet()) {
            oos.writeObject(entry.getKey());
            writeMap(oos, entry.getValue());
        }
    }

    public static DefaultBuildContextState loadFrom(Path stateFile) {
        // TODO verify stateFile location has not changed since last build
        // TODO wrap collections in corresponding immutable collections

        if (stateFile == null) {
            // transient build context
            return DefaultBuildContextState.emptyState();
        }

        try {
            // TODO does it matter if TCCL or super is called first?
            try (ObjectInputStream is =
                    new PathAwareInputStream(
                            stateFile.getFileSystem(), new BufferedInputStream(Files.newInputStream(stateFile))) {
                        @Override
                        protected Class<?> resolveClass(ObjectStreamClass desc)
                                throws IOException, ClassNotFoundException {
                            // TODO does it matter if TCCL or super is called first?
                            try {
                                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                                return tccl.loadClass(desc.getName());
                            } catch (ClassNotFoundException e) {
                                return super.resolveClass(desc);
                            }
                        }
                    }) {
                Map<String, Serializable> configuration = readMap(is);
                Set<Path> outputs = readSet(is);
                Map<Path, FileState> resources = readMap(is);
                Map<Path, Collection<Path>> resourceOutputs = readMultimap(is);
                Map<Path, Collection<Path>> outputInputs = invertMultimap(resourceOutputs);
                Map<Path, Map<String, Serializable>> resourceAttributes = readDoublemap(is);
                Map<Path, Collection<Message>> messages = readMultimap(is);

                DefaultBuildContextState state = new DefaultBuildContextState(
                        configuration, resources, outputs, resourceOutputs, outputInputs, resourceAttributes, messages);
                return state;
            }
            // ignore secondary exceptions
        } catch (FileNotFoundException e) {
            // this is expected, silently ignore
        } catch (RuntimeException e) {
            // this is a bug in our code, let it bubble up as build failure
            throw e;
        } catch (Exception e) {
            // this is almost certainly caused by incompatible state file, log and continue
        }
        return DefaultBuildContextState.emptyState();
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> readMap(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Map<K, V> map = new HashMap<>();
        int size = ois.readInt();
        for (int i = 0; i < size; i++) {
            K key = (K) ois.readObject();
            V value = (V) ois.readObject();
            map.put(key, value);
        }
        return Collections.unmodifiableMap(map);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, Collection<V>> readMultimap(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        Map<K, Collection<V>> mmap = new HashMap<>();
        int size = ois.readInt();
        for (int i = 0; i < size; i++) {
            K key = (K) ois.readObject();
            Collection<V> value = readCollection(ois);
            mmap.put(key, value);
        }
        return Collections.unmodifiableMap(mmap);
    }

    @SuppressWarnings("unchecked")
    private static <V> Collection<V> readCollection(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        int size = ois.readInt();
        if (size == 0) {
            return null;
        }
        Collection<V> collection = new ArrayList<V>();
        for (int i = 0; i < size; i++) {
            collection.add((V) ois.readObject());
        }
        return Collections.unmodifiableCollection(collection);
    }

    private static <V> Set<V> readSet(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Collection<V> collection = readCollection(ois);
        return collection != null ? Collections.unmodifiableSet(new HashSet<V>(collection)) : Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    private static <K, VK, VV> Map<K, Map<VK, VV>> readDoublemap(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        int size = ois.readInt();
        Map<K, Map<VK, VV>> dmap = new HashMap<>();
        for (int i = 0; i < size; i++) {
            K key = (K) ois.readObject();
            Map<VK, VV> value = readMap(ois);
            dmap.put(key, value);
        }
        return Collections.unmodifiableMap(dmap);
    }

    private static <K, V> Map<V, Collection<K>> invertMultimap(Map<K, Collection<V>> mmap) {
        Map<V, Collection<K>> inverted = new HashMap<>();
        for (Map.Entry<K, Collection<V>> entry : mmap.entrySet()) {
            for (V value : entry.getValue()) {
                Collection<K> keys = inverted.computeIfAbsent(value, k -> new ArrayList<>());
                keys.add(entry.getKey());
            }
        }
        return Collections.unmodifiableMap(inverted);
    }

    private static <K, V> boolean put(Map<K, Collection<V>> multimap, K key, V value) {
        Collection<V> values = multimap.computeIfAbsent(key, k -> new LinkedHashSet<V>());
        return values.add(value);
    }

    public String getStats() {
        return String.valueOf(configuration.size())
                + ' '
                + resources.size()
                + ' '
                + outputs.size()
                + ' '
                + resourceOutputs.size()
                + ' '
                + outputInputs.size()
                + ' '
                + resourceAttributes.size()
                + ' '
                + resourceMessages.size()
                + ' ';
    }

    //
    // getters and settings
    //

    // resources

    public void storeTo(OutputStream os) throws IOException {
        ObjectOutputStream oos = new PathAwareOutputStream(os);
        try {
            writeMap(oos, this.configuration);
            writeCollection(oos, this.outputs);
            writeMap(oos, this.resources);

            writeMultimap(oos, resourceOutputs);
            writeDoublemap(oos, resourceAttributes);
            writeMultimap(oos, resourceMessages);

        } finally {
            oos.flush();
        }
    }

    public void putResource(Path resource, FileState holder) {
        resources.put(resource, holder);
    }

    public FileState getResource(Path resource) {
        return resources.get(resource);
    }

    public void computeResourceIfAbsent(Path resource, Function<Path, FileState> supplier) {
        resources.computeIfAbsent(resource, supplier);
    }

    public boolean isResource(Path resource) {
        return resources.containsKey(resource);
    }

    public FileState removeResource(Path resource) {
        return resources.remove(resource);
    }

    // outputInputs

    public Map<Path, FileState> getResources() {
        return Collections.unmodifiableMap(resources);
    }

    // outputs

    public Collection<Path> getOutputInputs(Path outputFile) {
        return outputInputs.get(outputFile);
    }

    public Collection<Path> getOutputs() {
        return Collections.unmodifiableCollection(outputs);
    }

    public boolean isOutput(Path outputFile) {
        return outputs.contains(outputFile);
    }

    public boolean addOutput(Path output) {
        return outputs.add(output);
    }

    // resourceOutputs

    public boolean removeOutput(Path output) {
        return outputs.remove(output);
    }

    public boolean putResourceOutput(Path resource, Path output) {
        put(outputInputs, output, resource);
        return put(resourceOutputs, resource, output);
    }

    public Collection<Path> getResourceOutputs(Path resource) {
        return resourceOutputs.get(resource);
    }

    public Collection<Path> setResourceOutputs(Path resource, Collection<Path> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return resourceOutputs.remove(resource);
        }
        return resourceOutputs.put(resource, outputs);
    }

    public Collection<Path> removeResourceOutputs(Path resource) {
        Collection<Path> outputs = resourceOutputs.remove(resource);
        removeOutputInputs(outputs, resource);
        return outputs;
    }

    // resourceAttributes

    private void removeOutputInputs(Collection<Path> outputs, Path resource) {
        if (outputs == null) {
            return;
        }
        for (Path output : outputs) {
            Collection<Path> inputs = outputInputs.get(output);
            if (inputs == null || !inputs.remove(resource)) {
                throw new IllegalStateException();
            }
            if (inputs.isEmpty()) {
                outputInputs.remove(output);
            }
        }
    }

    public Map<String, Serializable> removeResourceAttributes(Path resource) {
        return resourceAttributes.remove(resource);
    }

    public Map<String, Serializable> getResourceAttributes(Path resource) {
        return resourceAttributes.get(resource);
    }

    public Serializable putResourceAttribute(Path resource, String key, Serializable value) {
        return resourceAttributes
                .computeIfAbsent(resource, k -> new LinkedHashMap<>())
                .put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getResourceAttribute(Path resource, String key) {
        return (T) resourceAttributes
                .getOrDefault(resource, Collections.emptyMap())
                .get(key);
    }

    // resourceMessages

    public Map<String, Serializable> setResourceAttributes(Path resource, Map<String, Serializable> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return resourceAttributes.remove(resource);
        }
        return resourceAttributes.put(resource, attributes);
    }

    public Collection<Message> removeResourceMessages(Path resource) {
        return resourceMessages.remove(resource);
    }

    public Collection<Message> getResourceMessages(Path resource) {
        return resourceMessages.get(resource);
    }

    public Collection<Message> setResourceMessages(Path resource, Collection<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return resourceMessages.remove(resource);
        }
        return resourceMessages.put(resource, messages);
    }

    public boolean addResourceMessage(Path resource, Message message) {
        return put(resourceMessages, resource, message);
    }

    public Map<Path, Collection<Message>> getResourceMessages() {
        return Collections.unmodifiableMap(resourceMessages);
    }

    /**
     * Path is not serializable
     * so this class is used as a workaround during serialization.
     */
    static class StoredPath implements Serializable {
        private final String path;

        StoredPath(Path path) {
            this.path = path.toString();
        }

        public Path toPath(FileSystem fileSystem) {
            return fileSystem.getPath(path);
        }
    }

    /**
     * FileTime is not serializable and Instant can not be used as a replacement,
     * so this class is used as a workaround during serialization.
     */
    static class StoredInstant implements Serializable {
        private final long seconds;
        private final int nanos;

        StoredInstant(FileTime time) {
            Instant instant = time.toInstant();
            this.seconds = instant.getEpochSecond();
            this.nanos = instant.getNano();
        }

        public FileTime toFileTime() {
            return FileTime.from(Instant.ofEpochSecond(seconds, nanos));
        }
    }

    static class PathAwareInputStream extends ObjectInputStream {
        private final FileSystem fileSystem;

        PathAwareInputStream(FileSystem fileSystem, InputStream in) throws IOException {
            super(in);
            this.fileSystem = fileSystem;
            enableResolveObject(true);
        }

        @Override
        protected Object resolveObject(Object obj) {
            return obj instanceof StoredPath
                    ? ((StoredPath) obj).toPath(fileSystem)
                    : obj instanceof StoredInstant ? ((StoredInstant) obj).toFileTime() : obj;
        }
    }

    static class PathAwareOutputStream extends ObjectOutputStream {
        PathAwareOutputStream(OutputStream os) throws IOException {
            super(new BufferedOutputStream(os));
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object obj) {
            return obj instanceof Path
                    ? new StoredPath((Path) obj)
                    : obj instanceof FileTime ? new StoredInstant((FileTime) obj) : obj;
        }

        @Override
        protected void writeObjectOverride(Object obj) throws IOException {
            super.writeObjectOverride(obj);
        }
    }
}
