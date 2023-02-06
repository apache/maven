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

import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Key;
import com.google.inject.Provider;
import junit.framework.TestCase;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.scope.WeakMojoExecutionListener;
import org.apache.maven.plugin.MojoExecutionException;

public class MojoExecutionScopeTest extends TestCase {
    public void testNestedEnter() throws Exception {
        MojoExecutionScope scope = new MojoExecutionScope();

        scope.enter();

        Object o1 = new Object();
        scope.seed(Object.class, o1);
        assertSame(o1, scope.scope(Key.get(Object.class), null).get());

        scope.enter();
        Object o2 = new Object();
        scope.seed(Object.class, o2);
        assertSame(o2, scope.scope(Key.get(Object.class), null).get());

        scope.exit();
        assertSame(o1, scope.scope(Key.get(Object.class), null).get());

        scope.exit();

        try {
            scope.exit();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    public void testMultiKeyInstance() throws Exception {
        MojoExecutionScope scope = new MojoExecutionScope();
        scope.enter();

        final AtomicInteger beforeExecution = new AtomicInteger();
        final AtomicInteger afterExecutionSuccess = new AtomicInteger();
        final AtomicInteger afterExecutionFailure = new AtomicInteger();
        final WeakMojoExecutionListener instance = new WeakMojoExecutionListener() {
            @Override
            public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {
                beforeExecution.incrementAndGet();
            }

            @Override
            public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
                afterExecutionSuccess.incrementAndGet();
            }

            @Override
            public void afterExecutionFailure(MojoExecutionEvent event) {
                afterExecutionFailure.incrementAndGet();
            }
        };
        assertSame(
                instance,
                scope.scope(Key.get(Object.class), new Provider<Object>() {
                            @Override
                            public Object get() {
                                return instance;
                            }
                        })
                        .get());
        assertSame(
                instance,
                scope.scope(Key.get(WeakMojoExecutionListener.class), new Provider<WeakMojoExecutionListener>() {
                            @Override
                            public WeakMojoExecutionListener get() {
                                return instance;
                            }
                        })
                        .get());

        final MojoExecutionEvent event = new MojoExecutionEvent(null, null, null, null);
        scope.beforeMojoExecution(event);
        scope.afterMojoExecutionSuccess(event);
        scope.afterExecutionFailure(event);

        assertEquals(1, beforeExecution.get());
        assertEquals(1, afterExecutionSuccess.get());
        assertEquals(1, afterExecutionFailure.get());

        scope.exit();
    }
}
