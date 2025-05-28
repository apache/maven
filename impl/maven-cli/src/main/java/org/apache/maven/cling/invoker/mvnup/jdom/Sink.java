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
package org.apache.maven.cling.invoker.mvnup.jdom;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Construct to accept collection of things.
 *
 * @param <T> the type of objects accepted in this Sink
 */
@FunctionalInterface
public interface Sink<T> extends AutoCloseable {
    void accept(T thing) throws IOException;

    default void accept(Stream<T> stream) throws IOException {
        try {
            stream.forEach(t -> {
                try {
                    accept(t);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    default void accept(Collection<T> things) throws IOException {
        requireNonNull(things, "things");
        try {
            for (T thing : things) {
                accept(thing);
            }
        } catch (IOException e) {
            cleanup(e);
            throw e;
        }
    }

    default void cleanup(Exception e) {}

    @Override
    default void close() throws Exception {}
}
