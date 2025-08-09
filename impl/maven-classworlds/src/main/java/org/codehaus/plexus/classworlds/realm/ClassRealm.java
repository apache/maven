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
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.strategy.Strategy;
import org.codehaus.plexus.classworlds.strategy.StrategyFactory;

/**
 * The class loading gateway. Each class realm has access to a base class loader, imports form zero or more other class
 * loaders, an optional parent class loader and of course its own class path. When queried for a class/resource, a class
 * realm will always query its base class loader first before it delegates to a pluggable strategy. The strategy in turn
 * controls the order in which imported class loaders, the parent class loader and the realm itself are searched. The
 * base class loader is assumed to be capable of loading of the bootstrap classes.
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @author Jason van Zyl
 */
public class ClassRealm extends URLClassLoader implements org.apache.maven.api.classworlds.ClassRealm {

    private ClassWorld world;

    private String id;

    private SortedSet<Entry> foreignImports;

    private SortedSet<Entry> parentImports;

    private Strategy strategy;

    private ClassLoader parentClassLoader;

    private static final boolean IS_PARALLEL_CAPABLE = Closeable.class.isAssignableFrom(URLClassLoader.class);

    private final ConcurrentMap<String, Object> lockMap;

    /**
     * Creates a new class realm.
     *
     * @param world           The class world this realm belongs to, must not be <code>null</code>.
     * @param id              The identifier for this realm, must not be <code>null</code>.
     * @param baseClassLoader The base class loader for this realm, may be <code>null</code> to use the bootstrap class
     *                        loader.
     */
    public ClassRealm(ClassWorld world, String id, ClassLoader baseClassLoader) {
        super(new URL[0], baseClassLoader);

        this.world = world;

        this.id = id;

        foreignImports = new TreeSet<>();

        strategy = StrategyFactory.getStrategy(this);

        lockMap = IS_PARALLEL_CAPABLE ? new ConcurrentHashMap<>() : null;

        if (IS_PARALLEL_CAPABLE) {
            // We must call super.getClassLoadingLock at least once
            // to avoid NPE in super.loadClass.
            super.getClassLoadingLock(getClass().getName());
        }
    }

    public String getId() {
        return this.id;
    }

    public ClassWorld getWorld() {
        return this.world;
    }

    /**
     * Returns the underlying ClassLoader for this realm.
     * <p>
     * This method allows access to the actual ClassLoader implementation
     * while maintaining API abstraction. Since ClassRealm extends URLClassLoader,
     * this method returns {@code this}.
     * </p>
     *
     * @return the underlying ClassLoader (this instance)
     */
    public ClassLoader getClassLoader() {
        return this;
    }

    public void importFromParent(String packageName) {
        if (parentImports == null) {
            parentImports = new TreeSet<>();
        }

        parentImports.add(new Entry(null, packageName));
    }

    boolean isImportedFromParent(String name) {
        if (parentImports != null && !parentImports.isEmpty()) {
            for (Entry entry : parentImports) {
                if (entry.matches(name)) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }

    public void importFrom(String realmId, String packageName) throws NoSuchRealmException {
        importFrom(getWorld().getRealm(realmId), packageName);
    }

    public void importFrom(ClassLoader classLoader, String packageName) {
        foreignImports.add(new Entry(classLoader, packageName));
    }

    public ClassLoader getImportClassLoader(String name) {
        for (Entry entry : foreignImports) {
            if (entry.matches(name)) {
                return entry.getClassLoader();
            }
        }

        return null;
    }

    public Collection<ClassRealm> getImportRealms() {
        Collection<ClassRealm> importRealms = new HashSet<>();

        for (Entry entry : foreignImports) {
            if (entry.getClassLoader() instanceof ClassRealm) {
                importRealms.add((ClassRealm) entry.getClassLoader());
            }
        }

        return importRealms;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setParentClassLoader(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }

    public ClassLoader getParentClassLoader() {
        return parentClassLoader;
    }

    public void setParentRealm(ClassRealm realm) {
        this.parentClassLoader = realm;
    }

    public ClassRealm getParentRealm() {
        return (parentClassLoader instanceof ClassRealm) ? (ClassRealm) parentClassLoader : null;
    }

    // Implementation of the original method signature for backward compatibility
    public ClassRealm createChildRealm(String id) throws DuplicateRealmException {
        ClassRealm childRealm = getWorld().newRealm(id, (ClassLoader) null);
        childRealm.setParentRealm(this);
        return childRealm;
    }

    public void addURL(URL url) {
        String urlStr = url.toExternalForm();

        if (urlStr.startsWith("jar:") && urlStr.endsWith("!/")) {
            urlStr = urlStr.substring(4, urlStr.length() - 2);

            try {
                url = new URL(urlStr);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        super.addURL(url);
    }

    // ----------------------------------------------------------------------
    // We delegate to the Strategy here so that we can change the behavior
    // of any existing ClassRealm.
    // ----------------------------------------------------------------------

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (IS_PARALLEL_CAPABLE) {
            return unsynchronizedLoadClass(name, resolve);

        } else {
            synchronized (this) {
                return unsynchronizedLoadClass(name, resolve);
            }
        }
    }

    private Class<?> unsynchronizedLoadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            // first, try loading bootstrap classes
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            // next, try loading via imports, self and parent as controlled by strategy
            return strategy.loadClass(name);
        }
    }

    // overwrites
    // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ClassLoader.html#findClass(java.lang.String,java.lang.String)
    // introduced in Java9
    protected Class<?> findClass(String moduleName, String name) {
        if (moduleName != null) {
            return null;
        }
        try {
            return findClassInternal(name);
        } catch (ClassNotFoundException e) {
            try {
                return strategy.getRealm().findClass(name);
            } catch (ClassNotFoundException nestedException) {
                return null;
            }
        }
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        /*
         * NOTE: This gets only called from ClassLoader.loadClass(Class, boolean) while we try to check for bootstrap
         * stuff. Don't scan our class path yet, loadClassFromSelf() will do this later when called by the strategy.
         */
        throw new ClassNotFoundException(name);
    }

    protected Class<?> findClassInternal(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    public URL getResource(String name) {
        URL resource = super.getResource(name);
        return resource != null ? resource : strategy.getResource(name);
    }

    public URL findResource(String name) {
        return super.findResource(name);
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        Collection<URL> resources = new LinkedHashSet<>(Collections.list(super.getResources(name)));
        resources.addAll(Collections.list(strategy.getResources(name)));
        return Collections.enumeration(resources);
    }

    public Enumeration<URL> findResources(String name) throws IOException {
        return super.findResources(name);
    }

    // ----------------------------------------------------------------------------
    // Display methods
    // ----------------------------------------------------------------------------

    public void display() {
        display(System.out);
    }

    public void display(PrintStream out) {
        out.println("-----------------------------------------------------");

        for (ClassRealm cr = this; cr != null; cr = (ClassRealm) cr.getParentRealm()) {
            out.println("realm =    " + cr.getId());
            out.println("strategy = " + cr.getStrategy().getClass().getName());

            showUrls(cr, out);

            out.println();
        }

        out.println("-----------------------------------------------------");
    }

    private static void showUrls(ClassRealm classRealm, PrintStream out) {
        URL[] urls = classRealm.getURLs();

        for (int i = 0; i < urls.length; i++) {
            out.println("urls[" + i + "] = " + urls[i]);
        }

        out.println("Number of foreign imports: " + classRealm.foreignImports.size());

        for (Entry entry : classRealm.foreignImports) {
            out.println("import: " + entry);
        }

        if (classRealm.parentImports != null) {
            out.println("Number of parent imports: " + classRealm.parentImports.size());

            for (Entry entry : classRealm.parentImports) {
                out.println("import: " + entry);
            }
        }
    }

    public String toString() {
        return "ClassRealm[" + getId() + ", parent: " + getParentClassLoader() + "]";
    }

    // ---------------------------------------------------------------------------------------------
    // Search methods that can be ordered by strategies to load a class
    // ---------------------------------------------------------------------------------------------

    public Class<?> loadClassFromImport(String name) {
        ClassLoader importClassLoader = getImportClassLoader(name);

        if (importClassLoader != null) {
            try {
                return importClassLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        return null;
    }

    public Class<?> loadClassFromSelf(String name) {
        synchronized (getClassRealmLoadingLock(name)) {
            try {
                Class<?> clazz = findLoadedClass(name);

                if (clazz == null) {
                    clazz = findClassInternal(name);
                }

                return clazz;
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }

    private Object getClassRealmLoadingLock(String name) {
        if (IS_PARALLEL_CAPABLE) {
            return getClassLoadingLock(name);
        } else {
            return this;
        }
    }

    @Override
    protected Object getClassLoadingLock(String name) {
        if (IS_PARALLEL_CAPABLE) {
            Object newLock = new Object();
            Object lock = lockMap.putIfAbsent(name, newLock);
            return (lock == null) ? newLock : lock;
        }
        return this;
    }

    public Class<?> loadClassFromParent(String name) {
        ClassLoader parent = getParentClassLoader();

        if (parent != null && isImportedFromParent(name)) {
            try {
                return parent.loadClass(name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // Search methods that can be ordered by strategies to get a resource
    // ---------------------------------------------------------------------------------------------

    public URL loadResourceFromImport(String name) {
        ClassLoader importClassLoader = getImportClassLoader(name);

        if (importClassLoader != null) {
            return importClassLoader.getResource(name);
        }

        return null;
    }

    public URL loadResourceFromSelf(String name) {
        return findResource(name);
    }

    public URL loadResourceFromParent(String name) {
        ClassLoader parent = getParentClassLoader();

        if (parent != null && isImportedFromParent(name)) {
            return parent.getResource(name);
        } else {
            return null;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Search methods that can be ordered by strategies to get resources
    // ---------------------------------------------------------------------------------------------

    public Enumeration<URL> loadResourcesFromImport(String name) {
        ClassLoader importClassLoader = getImportClassLoader(name);

        if (importClassLoader != null) {
            try {
                return importClassLoader.getResources(name);
            } catch (IOException e) {
                return null;
            }
        }

        return null;
    }

    public Enumeration<URL> loadResourcesFromSelf(String name) {
        try {
            return findResources(name);
        } catch (IOException e) {
            return null;
        }
    }

    public Enumeration<URL> loadResourcesFromParent(String name) {
        ClassLoader parent = getParentClassLoader();

        if (parent != null && isImportedFromParent(name)) {
            try {
                return parent.getResources(name);
            } catch (IOException e) {
                // eat it
            }
        }

        return null;
    }

    static {
        if (IS_PARALLEL_CAPABLE) // Avoid running this method on older jdks
        {
            registerAsParallelCapable();
        }
    }
}
