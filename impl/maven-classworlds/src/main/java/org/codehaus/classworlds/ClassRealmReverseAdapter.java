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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * A reverse adapter for ClassRealms.
 *
 * <p><b>Note:</b> This is a legacy internal class provided for backward compatibility with Maven 2.
 * New code should avoid using this adapter.</p>
 *
 * @author Andrew Williams
 * @deprecated This is a legacy internal class.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@Deprecated
public class ClassRealmReverseAdapter extends org.codehaus.plexus.classworlds.realm.ClassRealm {

    public static ClassRealmReverseAdapter getInstance(ClassRealm oldRealm) {
        return new ClassRealmReverseAdapter(oldRealm);
    }

    private final ClassRealm realm;

    private ClassRealmReverseAdapter(ClassRealm oldRealm) {
        super(ClassWorldReverseAdapter.getInstance(oldRealm.getWorld()), oldRealm.getId(), oldRealm.getClassLoader());
        this.realm = oldRealm;
    }

    public String getId() {
        return realm.getId();
    }

    public org.codehaus.plexus.classworlds.ClassWorld getWorld() {
        return ClassWorldReverseAdapter.getInstance(realm.getWorld());
    }

    public void importFrom(String realmId, String pkgName)
            throws org.codehaus.plexus.classworlds.realm.NoSuchRealmException {
        try {
            realm.importFrom(realmId, pkgName);
        } catch (NoSuchRealmException e) {
            throw new org.codehaus.plexus.classworlds.realm.NoSuchRealmException(getWorld(), e.getId());
        }
    }

    public void addURL(URL constituent) {
        realm.addConstituent(constituent);
    }

    public org.codehaus.plexus.classworlds.realm.ClassRealm locateSourceRealm(String className) {
        return getInstance(realm.locateSourceRealm(className));
    }

    public void setParentRealm(org.codehaus.plexus.classworlds.realm.ClassRealm classRealm) {
        realm.setParent(ClassRealmAdapter.getInstance(classRealm));
    }

    public org.codehaus.plexus.classworlds.realm.ClassRealm createChildRealm(String id)
            throws org.codehaus.plexus.classworlds.realm.DuplicateRealmException {
        try {
            return getInstance(realm.createChildRealm(id));
        } catch (DuplicateRealmException e) {
            throw new org.codehaus.plexus.classworlds.realm.DuplicateRealmException(getWorld(), e.getId());
        }
    }

    public ClassLoader getClassLoader() {
        return realm.getClassLoader();
    }

    public org.codehaus.plexus.classworlds.realm.ClassRealm getParentRealm() {
        return getInstance(realm.getParent());
    }

    public URL[] getURLs() {
        return realm.getConstituents();
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return realm.loadClass(name);
    }

    public URL getResource(String name) {
        return realm.getResource(name);
    }

    @SuppressWarnings("unchecked")
    public Enumeration findResources(String name) throws IOException {
        return realm.findResources(name);
    }

    public InputStream getResourceAsStream(String name) {
        return realm.getResourceAsStream(name);
    }

    public void display() {
        realm.display();
    }

    public boolean equals(Object o) {
        if (!(o instanceof ClassRealm)) return false;

        return getId().equals(((ClassRealm) o).getId());
    }
}
