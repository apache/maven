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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.maven.api.annotations.ThreadSafe;

/**
 * Thread-safe cache manager for model objects.
 * Uses weak references to allow garbage collection of unused objects.
 */
@ThreadSafe
class CacheManager {
    private static final CacheManager INSTANCE = new CacheManager();

    // Cache of object instances
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Integer, List<CacheReference>>> instanceCache =
            new ConcurrentHashMap<>();

    // Cache of reflected fields per class
    private final ConcurrentHashMap<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();

    // Reference queue
    private final ReferenceQueue<Cacheable> refQueue = new ReferenceQueue<>();

    private CacheManager() {
        // Start a background thread to process cleared references
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    CacheReference ref = (CacheReference) refQueue.remove();
                    ConcurrentHashMap<Integer, List<CacheReference>> classCache = instanceCache.get(ref.clazz);
                    if (classCache != null) {
                        classCache.computeIfPresent(ref.hash, (k, list) -> {
                            synchronized (list) {
                                list.remove(ref);
                                return list.isEmpty() ? null : list;
                            }
                        });
                        if (classCache.isEmpty()) {
                            instanceCache.remove(ref.clazz);
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
        ConcurrentHashMap<Integer, List<CacheReference>> classCache =
                instanceCache.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());

        int cacheHash = obj.cacheIdentityHash();

        List<CacheReference> refs = classCache.compute(cacheHash, (k, oldList) -> {
            if (oldList == null) {
                oldList = new ArrayList<>();
            }
            List<CacheReference> newList = new ArrayList<>(oldList); // Copy first
            newList.removeIf(ref -> ref.get() == null); // Safe to modify copy
            return newList;
        });

        synchronized (refs) {
            for (WeakReference<Cacheable> ref : refs) {
                Cacheable cached = ref.get();
                if (cached != null && obj.cacheEquals(cached)) {
                    return (T) cached;
                }
            }

            refs.add(new CacheReference(obj, refQueue, clazz, cacheHash));
            return obj;
        }
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
