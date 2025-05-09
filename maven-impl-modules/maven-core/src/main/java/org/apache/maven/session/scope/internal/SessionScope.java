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
package org.apache.maven.session.scope.internal;

import java.lang.annotation.Annotation;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.name.Named;

/**
 * SessionScope
 */
public class SessionScope extends org.apache.maven.impl.di.SessionScope implements Scope {

    public <T> void seed(Class<T> clazz, Provider<T> value) {
        getScopeState().seed(clazz, value::get);
    }

    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        Object qualifier = key.getAnnotation() instanceof Named n ? n.value() : key.getAnnotation();
        org.apache.maven.di.Key<T> k =
                org.apache.maven.di.Key.ofType(key.getTypeLiteral().getType(), qualifier);
        return scope(k, unscoped::get)::get;
    }

    public static <T> Provider<T> seededKeyProvider(Class<? extends T> clazz) {
        return SessionScope.<T>seededKeySupplier(clazz)::get;
    }

    protected boolean isTypeAnnotation(Class<? extends Annotation> annotationType) {
        return "org.apache.maven.api.di.Typed".equals(annotationType.getName())
                || "org.eclipse.sisu.Typed".equals(annotationType.getName())
                || "javax.enterprise.inject.Typed".equals(annotationType.getName())
                || "jakarta.enterprise.inject.Typed".equals(annotationType.getName());
    }
}
