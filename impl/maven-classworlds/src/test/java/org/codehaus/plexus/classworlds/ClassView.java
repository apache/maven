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

public class ClassView {
    /**
     * * Formats Class information for debug output purposes.
     * *
     * * @param clz  the Class to print information for
     * *
     * * @return a String describing the Class in detail
     */
    public static String toString(Class<?> clz) {
        if (clz.isPrimitive()) {
            return clz.toString();
        } else if (clz.isArray()) {
            return "Array of " + toString(clz.getComponentType());
        } else if (clz.isInterface()) {
            return toInterfaceString(clz, "");
        } else {
            return toClassString(clz, "");
        }
    }

    /**
     * * Formats Class information for debug output purposes.
     * *
     * * @param clz      the Class to print information for
     * * @param sIndent  the indentation to precede each line of output
     * *
     * * @return a String describing the Class in detail
     */
    private static String toClassString(Class<?> clz, String sIndent) {
        StringBuilder sb = new StringBuilder();
        sb.append(sIndent)
                .append("Class ")
                .append(clz.getName())
                .append("  (")
                .append(toString(clz.getClassLoader()))
                .append(')');

        sIndent += "  ";

        Class<?>[] aclz = clz.getInterfaces();
        for (Class<?> aClass : aclz) {
            sb.append('\n').append(toInterfaceString(aClass, sIndent));
        }

        clz = clz.getSuperclass();
        if (clz != null) {
            sb.append('\n').append(toClassString(clz, sIndent));
        }

        return sb.toString();
    }

    /**
     * * Formats interface information for debug output purposes.
     * *
     * * @param clz      the interface Class to print information for
     * * @param sIndent  the indentation to precede each line of output
     * *
     * * @return a String describing the interface Class in detail
     */
    private static String toInterfaceString(Class<?> clz, String sIndent) {
        StringBuilder sb = new StringBuilder();
        sb.append(sIndent)
                .append("Interface ")
                .append(clz.getName())
                .append("  (")
                .append(toString(clz.getClassLoader()))
                .append(')');

        Class<?>[] aclz = clz.getInterfaces();
        for (Class<?> aClass : aclz) {
            clz = aClass;

            sb.append('\n').append(toInterfaceString(clz, sIndent + "  "));
        }

        return sb.toString();
    }

    /**
     * * Format a description for the specified ClassLoader object.
     * *
     * * @param loader  the ClassLoader instance (or null)
     * *
     * * @return a String description of the ClassLoader
     */
    private static String toString(ClassLoader loader) {
        if (loader == null) {
            return "System ClassLoader";
        }

        return "ClassLoader class=" + loader.getClass().getName() + ", hashCode=" + loader.hashCode();
    }
}
