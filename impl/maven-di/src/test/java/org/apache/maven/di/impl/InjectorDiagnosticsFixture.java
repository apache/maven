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

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.api.di.Singleton;

/** Shared environment and components for injector diagnostic tests. */
final class InjectorDiagnosticsFixture implements AutoCloseable {
    static final String MAVEN_DI_DEBUG = "maven.di.debug";

    private final String previousProperty = System.getProperty(MAVEN_DI_DEBUG);
    private final PrintStream previousOutput = System.out;

    InjectorDiagnosticsFixture() {
        System.setProperty(MAVEN_DI_DEBUG, "true");
    }

    @Override
    public void close() {
        if (previousProperty == null) {
            System.clearProperty(MAVEN_DI_DEBUG);
        } else {
            System.setProperty(MAVEN_DI_DEBUG, previousProperty);
        }
        System.setOut(previousOutput);
    }

    static String body(String dump) {
        return dump.substring(dump.indexOf("{\n") + 2, dump.lastIndexOf("\n}"));
    }

    static List<String> bindingRows(String dump) {
        return body(dump)
                .lines()
                .filter(line -> line.matches("    (?:[+~-] )?candidate: .*"))
                .toList();
    }

    static Class<?> newRealmType() throws IOException {
        String name = Hostile.class.getName();
        byte[] bytes;
        try (var input = Hostile.class.getResourceAsStream("/" + name.replace('.', '/') + ".class")) {
            bytes = input.readAllBytes();
        }
        return new ClassLoader() {
            Class<?> define() {
                return defineClass(name, bytes, 0, bytes.length);
            }
        }.define();
    }

    static class RecordingInjector extends InjectorImpl {
        final List<String> dumps = new CopyOnWriteArrayList<>();

        void checkpoint() {
            debugBindings();
        }

        @Override
        void writeDump(PrintStream output, String dump) {
            dumps.add(dump);
        }
    }

    public static class Hostile {
        @Override
        public String toString() {
            throw new AssertionError("component toString");
        }
    }

    @Singleton
    static class Unconstructed {
        static final AtomicInteger CALLS = new AtomicInteger();

        Unconstructed() {
            CALLS.incrementAndGet();
        }
    }
}
