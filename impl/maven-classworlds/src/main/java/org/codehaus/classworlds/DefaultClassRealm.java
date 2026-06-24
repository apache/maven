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
package org.codehaus.classworlds;

/*
 * Copyright 2001-2010 Codehaus Foundation.
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

/**
 * A compatibility wrapper for {@link org.codehaus.plexus.classworlds.realm.ClassRealm}
 * provided for legacy code.
 *
 * <p><b>Note:</b> This is a legacy class provided for backward compatibility with Maven 2.
 * New code should use {@link org.codehaus.plexus.classworlds.realm.ClassRealm}.</p>
 *
 * @author Andrew Williams
 * @deprecated Use {@link org.codehaus.plexus.classworlds.realm.ClassRealm}
 */
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

@SuppressWarnings("rawtypes")
@Deprecated
public class DefaultClassRealm implements ClassRealm {
    private final ClassRealmAdapter adapter;

    public DefaultClassRealm(ClassWorld world, String id) {
        this(world, id, null);
    }

    public DefaultClassRealm(ClassWorld world, String id, ClassLoader foreignClassLoader) {
        this.adapter = ClassRealmAdapter.getInstance(new org.codehaus.plexus.classworlds.realm.ClassRealm(
                ClassWorldReverseAdapter.getInstance(world), id, foreignClassLoader));
    }

    public URL[] getConstituents() {
        return adapter.getConstituents();
    }

    public ClassRealm getParent() {
        return adapter.getParentRealm();
    }

    public void setParent(ClassRealm parent) {
        adapter.setParent(parent);
    }

    public String getId() {
        return adapter.getId();
    }

    public ClassWorld getWorld() {
        return adapter.getWorld();
    }

    public void importFrom(String realmId, String packageName) throws NoSuchRealmException {
        adapter.importFrom(realmId, packageName);
    }

    public void addConstituent(URL constituent) {
        adapter.addConstituent(constituent);
    }

    /**
     *  Adds a byte[] class definition as a constituent for locating classes.
     *  Currently uses BytesURLStreamHandler to hold a reference of the byte[] in memory.
     *  This ensures we have a unifed URL resource model for all constituents.
     *  The code to cache to disk is commented out - maybe a property to choose which method?
     *
     *  @param constituent class name
     *  @param b the class definition as a byte[]
     *  @throws ClassNotFoundException when class couldn't be loaded
     */
    public void addConstituent(String constituent, byte[] b) throws ClassNotFoundException {
        try {
            File path, file;
            if (constituent.lastIndexOf('.') != -1) {
                path = new File("byteclass/"
                        + constituent
                                .substring(0, constituent.lastIndexOf('.') + 1)
                                .replace('.', File.separatorChar));

                file = new File(path, constituent.substring(constituent.lastIndexOf('.') + 1) + ".class");
            } else {
                path = new File("byteclass/");

                file = new File(path, constituent + ".class");
            }

            addConstituent(new URL(null, file.toURI().toURL().toExternalForm(), new BytesURLStreamHandler(b)));
        } catch (java.io.IOException e) {
            throw new ClassNotFoundException("Couldn't load byte stream.", e);
        }
    }

    public ClassRealm locateSourceRealm(String classname) {
        return adapter.locateSourceRealm(classname);
    }

    public ClassLoader getClassLoader() {
        return adapter.getClassLoader();
    }

    public ClassRealm createChildRealm(String id) throws DuplicateRealmException {
        return adapter.createChildRealm(id);
    }

    // ----------------------------------------------------------------------
    // ClassLoader API
    // ----------------------------------------------------------------------

    public Class loadClass(String name) throws ClassNotFoundException {
        return adapter.loadClass(name);
    }

    public URL getResource(String name) {
        return adapter.getResource(name);
    }

    public InputStream getResourceAsStream(String name) {
        return adapter.getResourceAsStream(name);
    }

    public Enumeration findResources(String name) throws IOException {
        return adapter.findResources(name);
    }

    public void display() {
        adapter.display();
    }
}
