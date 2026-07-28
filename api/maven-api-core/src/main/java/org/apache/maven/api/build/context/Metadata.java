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
package org.apache.maven.api.build.context;

import java.nio.file.Path;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Provider;

/**
 * Wraps a registered resource with its file path and change status,
 * and provides a {@link #process()} method to obtain the full resource handle.
 *
 * @param <R> the resource type ({@link Input} or {@link Output})
 * @since 4.0.0
 */
@Experimental
@NotThreadSafe
@Provider
public interface Metadata<R extends Resource> {

    /**
     * {@return the path of the registered resource}
     */
    @Nonnull
    Path getPath();

    /**
     * {@return the change status of the resource relative to the previous build}
     */
    @Nonnull
    Status getStatus();

    /**
     * Marks this resource for processing and returns the full resource handle.
     *
     * @return the resource to process
     */
    @Nonnull
    R process();
}
