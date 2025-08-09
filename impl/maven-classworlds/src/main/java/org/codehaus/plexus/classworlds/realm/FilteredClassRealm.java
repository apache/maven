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
package org.codehaus.plexus.classworlds.realm;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.function.Predicate;

import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Similar to {@link ClassRealm} but only exposing some resources of the underlying URL.
 * Only supposed to be called from {@link ClassWorld}.
 */
public class FilteredClassRealm extends ClassRealm {
    private final Predicate<String> filter;

    /**
     * Creates a new class realm.
     *
     * @param filter a predicate to apply to each resource name to determine if it should be loaded through this class loader
     * @param world The class world this realm belongs to, must not be <code>null</code>.
     * @param id The identifier for this realm, must not be <code>null</code>.
     * @param baseClassLoader The base class loader for this realm, may be <code>null</code> to use the bootstrap class
     *            loader.
     */
    public FilteredClassRealm(Predicate<String> filter, ClassWorld world, String id, ClassLoader baseClassLoader) {
        super(world, id, baseClassLoader);
        this.filter = filter;
    }

    @Override
    protected Class<?> findClassInternal(String name) throws ClassNotFoundException {
        String resourceName = name.replace('.', '/').concat(".class");
        if (!filter.test(resourceName)) {
            throw new ClassNotFoundException(name);
        }
        return super.findClassInternal(name);
    }

    @Override
    public URL findResource(String name) {
        if (!filter.test(name)) {
            return null;
        }
        return super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        if (!filter.test(name)) {
            return Collections.emptyEnumeration();
        }
        return super.findResources(name);
    }
}
