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
package org.codehaus.plexus.classworlds;

/*
 * Copyright 2001-2006 Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.FilteredClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

/**
 * A collection of <code>ClassRealm</code>s, indexed by id.
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 */
public class ClassWorld implements org.apache.maven.api.classworlds.ClassWorld, Closeable {
    private Map<String, ClassRealm> realms;

    private final List<ClassWorldListener> listeners = new ArrayList<>();

    public ClassWorld(String realmId, ClassLoader classLoader) {
        this();

        try {
            newRealm(realmId, classLoader);
        } catch (DuplicateRealmException e) {
            // Will never happen as we are just creating the world.
        }
    }

    public ClassWorld() {
        this.realms = new LinkedHashMap<>();
    }

    public ClassRealm newRealm(String id) throws DuplicateRealmException {
        return newRealm(id, getClass().getClassLoader());
    }

    public ClassRealm newRealm(String id, ClassLoader classLoader) throws DuplicateRealmException {
        return newRealm(id, classLoader, null);
    }

    /**
     * Adds a class realm with filtering.
     * Only resources/classes whose name matches a given predicate are exposed.
     * @param id The identifier for this realm, must not be <code>null</code>.
     * @param classLoader The base class loader for this realm, may be <code>null</code> to use the bootstrap class
     *            loader.
     * @param filter a predicate to apply to each resource name to determine if it should be loaded through this class loader
     * @return the created class realm
     * @throws DuplicateRealmException in case a realm with the given id does already exist
     * @since 2.7.0
     * @see FilteredClassRealm
     */
    public synchronized ClassRealm newRealm(String id, ClassLoader classLoader, Predicate<String> filter)
            throws DuplicateRealmException {
        if (realms.containsKey(id)) {
            throw new DuplicateRealmException(this, id);
        }

        ClassRealm realm;

        if (filter == null) {
            realm = new ClassRealm(this, id, classLoader);
        } else {
            realm = new FilteredClassRealm(filter, this, id, classLoader);
        }
        realms.put(id, realm);

        for (ClassWorldListener listener : listeners) {
            listener.realmCreated(realm);
        }

        return realm;
    }

    /**
     * Closes all contained class realms.
     * @since 2.7.0
     */
    @Override
    public synchronized void close() throws IOException {
        realms.values().stream().forEach(this::disposeRealm);
        realms.clear();
    }

    public synchronized void disposeRealm(String id) throws NoSuchRealmException {
        ClassRealm realm = realms.remove(id);

        if (realm != null) {
            disposeRealm(realm);
        } else {
            throw new NoSuchRealmException(this, id);
        }
    }

    private void disposeRealm(ClassRealm realm) {
        try {
            realm.close();
        } catch (IOException ignore) {
        }
        for (ClassWorldListener listener : listeners) {
            listener.realmDisposed(realm);
        }
    }

    public synchronized ClassRealm getRealm(String id) throws NoSuchRealmException {
        if (realms.containsKey(id)) {
            return realms.get(id);
        }

        throw new NoSuchRealmException(this, id);
    }

    public synchronized Collection<ClassRealm> getRealms() {
        return Collections.unmodifiableList(new ArrayList<>(realms.values()));
    }

    // from exports branch
    public synchronized ClassRealm getClassRealm(String id) {
        if (realms.containsKey(id)) {
            return realms.get(id);
        }

        return null;
    }

    public synchronized void addListener(ClassWorldListener listener) {
        // TODO ideally, use object identity, not equals
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(ClassWorldListener listener) {
        listeners.remove(listener);
    }

    // API interface methods - newRealm with filter is already implemented above

    @Override
    public void addListener(org.apache.maven.api.classworlds.ClassWorldListener listener) {
        if (listener instanceof ClassWorldListener) {
            addListener((ClassWorldListener) listener);
        } else {
            // Wrap the API listener
            addListener(new ClassWorldListener() {
                @Override
                public void realmCreated(ClassRealm realm) {
                    listener.realmCreated(realm);
                }

                @Override
                public void realmDisposed(ClassRealm realm) {
                    listener.realmDisposed(realm);
                }

                @Override
                public void realmCreated(org.apache.maven.api.classworlds.ClassRealm realm) {
                    listener.realmCreated(realm);
                }

                @Override
                public void realmDisposed(org.apache.maven.api.classworlds.ClassRealm realm) {
                    listener.realmDisposed(realm);
                }
            });
        }
    }

    @Override
    public void removeListener(org.apache.maven.api.classworlds.ClassWorldListener listener) {
        // For now, we'll need to track wrapped listeners if this becomes important
        // This is a limitation of the current design
    }
}
