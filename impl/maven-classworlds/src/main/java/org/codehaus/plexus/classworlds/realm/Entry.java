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

/**
 * Import description entry.
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 */
class Entry implements Comparable<Entry> {

    final ClassLoader classLoader;

    final String pkgName;

    Entry(ClassLoader realm, String pkgName) {
        this.classLoader = realm;

        this.pkgName = pkgName;
    }

    // ------------------------------------------------------------
    //     Instance methods
    // ------------------------------------------------------------

    /**
     * Retrieve the class loader.
     *
     * @return The class loader.
     */
    ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * Retrieve the package name.
     *
     * @return The package name.
     */
    String getPackageName() {
        return this.pkgName;
    }

    /**
     * Determine if the class/resource name matches the package
     * described by this entry.
     *
     * @param name The class or resource name to test, must not be <code>null</code>.
     * @return <code>true</code> if this entry matches the
     *         classname, otherwise <code>false</code>.
     */
    boolean matches(String name) {
        String pkg = getPackageName();

        if (pkg.endsWith(".*")) {
            String pkgName;

            if (name.indexOf('/') < 0) {
                // a binary class name, e.g. java.lang.Object

                int index = name.lastIndexOf('.');
                pkgName = (index < 0) ? "" : name.substring(0, index);
            } else {
                // a resource name, e.g. java/lang/Object.class

                int index = name.lastIndexOf('/');
                pkgName = (index < 0) ? "" : name.substring(0, index).replace('/', '.');
            }

            return pkgName.length() == pkg.length() - 2 && pkg.regionMatches(0, pkgName, 0, pkgName.length());
        } else if (pkg.length() > 0) {
            if (name.indexOf('/') < 0) {
                // a binary class name, e.g. java.lang.Object

                if (name.startsWith(pkg)) {
                    if (name.length() == pkg.length()) {
                        // exact match of class name
                        return true;
                    } else if (name.charAt(pkg.length()) == '.') {
                        // prefix match of package name
                        return true;
                    } else if (name.charAt(pkg.length()) == '$') {
                        // prefix match of enclosing type
                        return true;
                    }
                }
            } else {
                // a resource name, e.g. java/lang/Object.class

                if (name.equals(pkg)) {
                    // exact match of resource name
                    return true;
                }

                pkg = pkg.replace('.', '/');

                if (name.startsWith(pkg) && name.length() > pkg.length()) {
                    if (name.charAt(pkg.length()) == '/') {
                        // prefix match of package directory
                        return true;
                    } else if (name.charAt(pkg.length()) == '$') {
                        // prefix match of nested class file
                        return true;
                    } else if (name.length() == pkg.length() + 6 && name.endsWith(".class")) {
                        // exact match of class file
                        return true;
                    }
                }
            }

            return false;
        } else {
            return true;
        }
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //     java.lang.Comparable
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     * Compare this entry to another for relative ordering.
     * <p/>
     * <p/>
     * The natural ordering of Entry objects is reverse-alphabetical
     * based upon package name.
     * </p>
     *
     * @param that The object to compare.
     * @return -1 if this object sorts before that object, 0
     *         if they are equal, or 1 if this object sorts
     *         after that object.
     */
    public int compareTo(Entry that) {
        // We are reverse sorting this list, so that
        // we get longer matches first:
        //
        //     com.werken.foo.bar
        //     com.werken.foo
        //     com.werken

        return -(getPackageName().compareTo(that.getPackageName()));
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //     java.lang.Object
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     * Test this entry for equality to another.
     * <p/>
     * <p/>
     * Consistent with {@link #compareTo}, this method tests
     * for equality purely on the package name.
     * </p>
     *
     * @param thatObj The object to compare
     * @return <code>true</code> if the two objects are
     *         semantically equivalent, otherwise <code>false</code>.
     */
    public boolean equals(Object thatObj) {
        Entry that = (Entry) thatObj;

        return getPackageName().equals(that.getPackageName());
    }

    /**
     * <p/>
     * Consistent with {@link #equals}, this method creates a hashCode
     * based on the packagename.
     * </p>
     */
    public int hashCode() {
        return getPackageName().hashCode();
    }

    public String toString() {
        return "Entry[import " + getPackageName() + " from realm " + getClassLoader() + "]";
    }
}
