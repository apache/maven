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
package org.apache.maven.di.impl;

import java.lang.annotation.Annotation;

import org.apache.maven.api.annotations.Nullable;

public final class Utils {

    public static String getDisplayString(Class<? extends Annotation> annotationType, @Nullable Annotation annotation) {
        if (annotation == null) {
            return "@" + ReflectionUtils.getDisplayName(annotationType);
        }
        String typeName = annotationType.getName();
        String str = annotation.toString();
        return str.startsWith("@" + typeName)
                ? "@" + ReflectionUtils.getDisplayName(annotationType) + str.substring(typeName.length() + 1)
                : str;
    }

    public static String getDisplayString(Object object) {
        if (object instanceof Class && ((Class<?>) object).isAnnotation()) {
            //noinspection unchecked
            return getDisplayString((Class<? extends Annotation>) object, null);
        }
        if (object instanceof Annotation) {
            Annotation annotation = (Annotation) object;
            return getDisplayString(annotation.annotationType(), annotation);
        }
        return object.toString();
    }

    public static boolean isMarker(Class<? extends Annotation> annotationType) {
        return annotationType.getDeclaredMethods().length == 0;
    }
}
