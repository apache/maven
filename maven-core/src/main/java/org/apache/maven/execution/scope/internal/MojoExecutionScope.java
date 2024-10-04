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
package org.apache.maven.execution.scope.internal;

import java.util.Collection;
import java.util.IdentityHashMap;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.name.Named;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.execution.scope.WeakMojoExecutionListener;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * MojoExecutionScope
 */
public class MojoExecutionScope extends org.apache.maven.internal.impl.di.MojoExecutionScope
        implements Scope, MojoExecutionListener {

    public <T> void seed(Class<T> clazz, Provider<T> value) {
        getScopeState().seed(clazz, value::get);
    }

    public <T> Provider<T> scope(final Key<T> key, Provider<T> unscoped) {
        Object qualifier = key.getAnnotation() instanceof Named n ? n.value() : key.getAnnotation();
        org.apache.maven.di.Key<T> k =
                org.apache.maven.di.Key.ofType(key.getTypeLiteral().getType(), qualifier);
        return scope(k, unscoped::get)::get;
    }

    public static <T> Provider<T> seededKeyProvider(Class<? extends T> clazz) {
        return MojoExecutionScope.<T>seededKeySupplier(clazz)::get;
    }

    public static <T> Provider<T> seededKeyProvider(Class<? extends T> clazz) {
        return () -> {
            throw new IllegalStateException(
                    "No instance of " + clazz.getName() + " is bound to the mojo execution scope.");
        };
    }

    public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {
        for (WeakMojoExecutionListener provided : getProvidedListeners()) {
            provided.beforeMojoExecution(event);
        }
    }

    public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
        for (WeakMojoExecutionListener provided : getProvidedListeners()) {
            provided.afterMojoExecutionSuccess(event);
        }
    }

    public void afterExecutionFailure(MojoExecutionEvent event) {
        for (WeakMojoExecutionListener provided : getProvidedListeners()) {
            provided.afterExecutionFailure(event);
        }
    }

    private Collection<WeakMojoExecutionListener> getProvidedListeners() {
        // the same instance can be provided multiple times under different Key's
        // deduplicate instances to avoid redundant beforeXXX/afterXXX callbacks
        IdentityHashMap<WeakMojoExecutionListener, Object> listeners = new IdentityHashMap<>();
        for (Object provided : getScopeState().provided()) {
            if (provided instanceof WeakMojoExecutionListener) {
                listeners.put((WeakMojoExecutionListener) provided, null);
            }
        }
        return listeners.keySet();
    }
}
