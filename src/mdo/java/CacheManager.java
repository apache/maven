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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.maven.api.annotations.ThreadSafe;

/**
 * Thread-safe cache manager for model objects.
 * Uses weak references to allow garbage collection of unused objects.
 */
@ThreadSafe
class CacheManager {
    private static final CacheManager INSTANCE = new CacheManager();
    private static final int BATCH_SIZE = 32;
    private static final long CLEANUP_WAIT_MS = 100;  // Time to wait when queue is empty

    // Cache of object instances
    private final ConcurrentHashMap<Class<?>, WeakReference<ConcurrentHashMap<Integer, CacheNode>>> instanceCache =
            new ConcurrentHashMap<>();

    // Cache of reflected fields per class
    private final ConcurrentHashMap<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();

    // Reference queue
    private final ReferenceQueue<Cacheable> refQueue = new ReferenceQueue<>();

    /**
     * Lightweight structure to handle hash collisions.
     * Acts as both a WeakReference and a linked list node.
     */
    private static class CacheNode extends WeakReference<Cacheable> {
        final Class<?> clazz;
        final int hash;
        volatile CacheNode next;  // null if this is the only element

        CacheNode(Cacheable referent, ReferenceQueue<Cacheable> q, Class<?> clazz, int hash) {
            super(referent, q);
            this.clazz = clazz;
            this.hash = hash;
        }

        /**
         * Finds a matching object in this chain of nodes.
         * Also cleans up any cleared references it encounters.
         * Returns the node containing the match, or null if none found.
         */
        CacheNode findMatch(Cacheable obj, ConcurrentHashMap<Integer, CacheNode> classCache) {
            CacheNode prev = null;
            CacheNode current = this;

            while (current != null) {
                Cacheable cached = current.get();
                CacheNode next = current.next;

                if (cached == null) {
                    // Remove cleared reference
                    if (prev == null) {
                        // This is the head node
                        if (next == null) {
                            // Last node in chain, remove entire entry
                            classCache.remove(hash);
                        } else {
                            // Replace head with next node
                            classCache.replace(hash, this, next);
                        }
                    } else {
                        // Skip this node
                        prev.next = next;
                    }
                } else if (obj.cacheEquals(cached)) {
                    return current;
                } else {
                    prev = current;
                }
                current = next;
            }
            return null;
        }
    }

    private CacheManager() {
        // Start a background thread to process cleared references
        Thread cleanupThread = new Thread(() -> {
            List<CacheNode> refBatch = new ArrayList<>(BATCH_SIZE);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Try to collect a batch of references
                    refBatch.clear();
                    CacheNode ref;

                    // Poll for references until either batch is full or queue is empty
                    while (refBatch.size() < BATCH_SIZE &&
                            (ref = (CacheNode)refQueue.poll()) != null) {
                        refBatch.add(ref);
                    }

                    if (refBatch.isEmpty()) {
                        // Queue is empty, wait for new references
                        ref = (CacheNode)refQueue.remove(CLEANUP_WAIT_MS);
                        if (ref != null) {
                            refBatch.add(ref);
                        }
                    }

                    // Process the batch
                    if (!refBatch.isEmpty()) {
                        // Group references by class for more efficient processing
                        Map<Class<?>, List<CacheNode>> byClass = refBatch.stream()
                                .collect(Collectors.groupingBy(node -> node.clazz));

                        for (var entry : byClass.entrySet()) {
                            WeakReference<ConcurrentHashMap<Integer, CacheNode>> weakClassCache =
                                    instanceCache.get(entry.getKey());
                            if (weakClassCache != null) {
                                ConcurrentHashMap<Integer, CacheNode> classCache = weakClassCache.get();
                                if (classCache != null) {
                                    // Process all references for this class
                                    for (CacheNode node : entry.getValue()) {
                                        CacheNode head = classCache.get(node.hash);
                                        if (head != null) {
                                            head.findMatch(null, classCache);
                                        }
                                    }

                                    // If the class cache is empty, remove its weak reference
                                    if (classCache.isEmpty()) {
                                        instanceCache.remove(entry.getKey(), weakClassCache);
                                    }
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "CacheManager-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    public static CacheManager getInstance() {
        return INSTANCE;
    }

    /**
     * Gets all relevant fields for a class, using cached values when possible.
     * Includes all non-static, non-synthetic fields from the class and its superclasses.
     */
    private Field[] getFields(Class<?> clazz) {
        return fieldCache.computeIfAbsent(clazz, k -> {
            List<Field> fields = new ArrayList<>();
            Class<?> currentClass = k;

            // Collect all fields from class hierarchy
            while (currentClass != null) {
                for (Field field : currentClass.getDeclaredFields()) {
                    // Skip static and synthetic fields
                    if ((field.getModifiers() & (Modifier.STATIC | 0x00001000)) == 0) {
                        field.setAccessible(true);  // Do this once during caching
                        fields.add(field);
                    }
                }
                currentClass = currentClass.getSuperclass();
            }

            return fields.toArray(new Field[0]);
        });
    }

    /**
     * Computes a cache identity hash code by hashing all fields recursively.
     * Only processes objects that implement Cacheable.
     */
    public int computeCacheHash(Cacheable obj) {
        if (obj == null) {
            return 0;
        }

        try {
            int result = 1;
            for (Field field : getFields(obj.getClass())) {
                Object value = field.get(obj);

                int fieldHash;
                if (value instanceof List<?> list) {
                    fieldHash = 1;
                    for (Object element : list) {
                        if (element instanceof Cacheable cacheable) {
                            fieldHash = 31 * fieldHash + computeCacheHash(cacheable);
                        } else {
                            fieldHash = 31 * fieldHash + Objects.hashCode(element);
                        }
                    }
                } else if (value instanceof Cacheable cacheable) {
                    fieldHash = computeCacheHash(cacheable);
                } else {
                    fieldHash = Objects.hashCode(value);
                }

                result = 31 * result + fieldHash;
            }
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    /**
     * Compares two cacheable objects for equality by comparing all their fields recursively.
     */
    public boolean cacheEquals(Cacheable o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1.getClass() != o2.getClass()) {
            return false;
        }

        try {
            for (Field field : getFields(o1.getClass())) {
                Object v1 = field.get(o1);
                Object v2 = field.get(o2);

                if (v1 == v2) {
                    continue;
                }
                if (v1 == null || v2 == null) {
                    return false;
                }

                if (v1 instanceof List<?> list1 && v2 instanceof List<?> list2) {
                    if (list1.size() != list2.size()) {
                        return false;
                    }
                    for (int i = 0; i < list1.size(); i++) {
                        Object e1 = list1.get(i);
                        Object e2 = list2.get(i);
                        if (e1 instanceof Cacheable cacheable1 && e2 instanceof Cacheable) {
                            if (!cacheEquals(cacheable1, e2)) {
                                return false;
                            }
                        } else if (!Objects.equals(e1, e2)) {
                            return false;
                        }
                    }
                } else if (v1 instanceof Cacheable cacheable1 && v2 instanceof Cacheable) {
                    if (!cacheEquals(cacheable1, v2)) {
                        return false;
                    }
                } else if (!Objects.equals(v1, v2)) {
                    return false;
                }
            }
            return true;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to compare objects", e);
        }
    }

    /**
     * Caches a Cacheable object, returning either the existing instance or the new one.
     */
    @SuppressWarnings("unchecked")
    public <T extends Cacheable> T cached(T obj) {
        if (obj == null) {
            return null;
        }

        Class<?> clazz = obj.getClass();
        ConcurrentHashMap<Integer, CacheNode> classCache = getOrCreateClassCache(clazz);
        int cacheHash = obj.cacheIdentityHash();

        while (true) {
            CacheNode head = classCache.get(cacheHash);
            if (head == null) {
                // No existing entry, try to add new one
                CacheNode newNode = new CacheNode(obj, refQueue, clazz, cacheHash);
                if (classCache.putIfAbsent(cacheHash, newNode) == null) {
                    return obj;  // Successfully added
                }
                continue;  // Race condition, try again
            }

            // Look for existing match
            CacheNode match = head.findMatch(obj, classCache);
            if (match != null) {
                Cacheable cached = match.get();
                if (cached != null) {
                    return (T) cached;
                }
                continue;  // Reference was cleared, try again
            }

            // No match found, try to add to chain
            synchronized (head) {  // Synchronize on head node for chain modifications
                // Verify head is still valid
                if (classCache.get(cacheHash) != head) {
                    continue;  // Head changed, try again
                }

                // Add new node to chain
                CacheNode newNode = new CacheNode(obj, refQueue, clazz, cacheHash);
                newNode.next = head.next;
                head.next = newNode;
                return obj;
            }
        }
    }

    private ConcurrentHashMap<Integer, CacheNode> getOrCreateClassCache(Class<?> clazz) {
        WeakReference<ConcurrentHashMap<Integer, CacheNode>> weakRef = instanceCache.get(clazz);
        ConcurrentHashMap<Integer, CacheNode> classCache = (weakRef != null) ? weakRef.get() : null;

        if (classCache == null) {
            classCache = new ConcurrentHashMap<>();
            weakRef = new WeakReference<>(classCache);
            WeakReference<ConcurrentHashMap<Integer, CacheNode>> existing =
                    instanceCache.putIfAbsent(clazz, weakRef);

            if (existing != null) {
                ConcurrentHashMap<Integer, CacheNode> existingCache = existing.get();
                if (existingCache != null) {
                    classCache = existingCache;
                } else {
                    instanceCache.replace(clazz, existing, weakRef);
                }
            }
        }

        return classCache;
    }

    /**
     * Clears all cached instances.
     * Does not clear the field cache as that remains valid.
     */
    public void clear() {
        instanceCache.clear();
    }

    // Custom WeakReference that keeps track of its key for removal
    private static class CacheReference extends WeakReference<Cacheable> {
        private final Class<?> clazz;
        private final int hash;

        CacheReference(Cacheable referent, ReferenceQueue<Cacheable> q, Class<?> clazz, int hash) {
            super(referent, q);
            this.clazz = clazz;
            this.hash = hash;
        }
    }
}
