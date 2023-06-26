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
package org.apache.maven.plugin;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Supplier;

import org.apache.maven.internal.impl.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractMojoTest {
    @Test
    void getDefaultLogSystem() throws Throwable {
        ignoring( // ignore slf4j to get default behavior
                singletonList("org.slf4j."),
                singletonList("org/slf4j/impl/StaticLoggerBinder.class"),
                this::assertSystem);
    }

    @Test
    void getDefaultLogSystemOnMissingMavenCore() throws Throwable {
        ignoring(singletonList("org.apache.maven.internal.impl."), emptyList(), this::assertSystem);
    }

    @Test
    void getDefaultLogSlf4j() {
        final Log log = captureLog();
        assertTrue(Proxy.isProxyClass(log.getClass()));
        final org.apache.maven.api.plugin.Log delegate = ((Supplier<org.apache.maven.api.plugin.Log>) log).get();
        assertEquals(
                CaptureLogMojo.class.getName(),
                assertInstanceOf(DefaultLog.class, delegate).getLogger().getName());
    }

    private void assertSystem() {
        final Log log = captureLog();
        assertFalse(Proxy.isProxyClass(log.getClass()));
        assertEquals(
                "org.apache.maven.plugin.logging.SystemStreamLog",
                log.getClass().getName());
    }

    private Log captureLog() {
        final CaptureLogMojo mojo = new CaptureLogMojo();
        mojo.execute();
        assertNotNull(mojo.ref);
        return mojo.ref;
    }

    private void ignoring(final List<String> packageNames, final List<String> resources, final Executable test)
            throws Throwable {
        if (packageNames.isEmpty() && resources.isEmpty()) {
            test.execute();
            return;
        }

        final Thread thread = Thread.currentThread();
        final ClassLoader parent = thread.getContextClassLoader();
        final ClassLoader custom = new ClassLoader(parent) {
            @Override
            protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                if (name != null && packageNames.stream().anyMatch(name::startsWith)) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }

            @Override
            public Enumeration<URL> getResources(final String name) throws IOException {
                if (name != null && resources.stream().anyMatch(name::startsWith)) {
                    return emptyEnumeration();
                }
                return super.getResources(name);
            }
        };
        try {
            thread.setContextClassLoader(custom);

        } finally {
            thread.setContextClassLoader(parent);
        }
    }

    private static class CaptureLogMojo extends AbstractMojo {
        private Log ref;

        @Override
        public void execute() {
            ref = getLog();
        }
    }
}
