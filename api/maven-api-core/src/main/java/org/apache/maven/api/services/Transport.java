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
package org.apache.maven.api.services;

import java.io.Closeable;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Transport for specified remote repository (using provided remote repository base URI as root). Must be treated as a
 * resource, best in try-with-resource block.
 *
 * @since 4.0
 */
@Experimental
@Consumer
public interface Transport extends Closeable {
    /**
     * GETs the source URI content into target (does not have to exist, or will be overwritten if exist). The
     * source MUST BE relative from the {@link RemoteRepository#getUrl()} root.
     *
     * @return {@code true} if operation succeeded, {@code false} if source does not exist.
     * @throws RuntimeException If failed (and not due source not exists).
     */
    boolean get(@Nonnull URI relativeSource, @Nonnull Path target);

    /**
     * GETs the source URI content as byte array. The source MUST BE relative from the {@link RemoteRepository#getUrl()}
     * root.
     *
     * @return the byte array if operation succeeded, {@code null} if source does not exist.
     * @throws RuntimeException If failed (and not due source not exists).
     */
    @Nonnull
    Optional<byte[]> getBytes(@Nonnull URI relativeSource);

    /**
     * GETs the source URI content as string. The source MUST BE relative from the {@link RemoteRepository#getUrl()}
     * root.
     *
     * @return the string if operation succeeded, {@code null} if source does not exist.
     * @throws RuntimeException If failed (and not due source not exists).
     */
    @Nonnull
    Optional<String> getString(@Nonnull URI relativeSource, @Nonnull Charset charset);

    /**
     * GETs the source URI content as string using UTF8 charset. The source MUST BE relative from the
     * {@link RemoteRepository#getUrl()} root.
     *
     * @return the string if operation succeeded, {@code null} if source does not exist.
     * @throws RuntimeException If failed (and not due source not exists).
     */
    @Nonnull
    default Optional<String> getString(@Nonnull URI relativeSource) {
        return getString(relativeSource, StandardCharsets.UTF_8);
    }

    /**
     * PUTs the source file (must exist as file) to target URI. The target MUST BE relative from the
     * {@link RemoteRepository#getUrl()} root.
     *
     * @throws RuntimeException If PUT fails for any reason.
     */
    void put(@Nonnull Path source, @Nonnull URI relativeTarget);

    /**
     * PUTs the source byte array to target URI. The target MUST BE relative from the
     * {@link RemoteRepository#getUrl()} root.
     *
     * @throws RuntimeException If PUT fails for any reason.
     */
    void putBytes(@Nonnull byte[] source, @Nonnull URI relativeTarget);

    /**
     * PUTs the source string to target URI. The target MUST BE relative from the
     * {@link RemoteRepository#getUrl()} root.
     *
     * @throws RuntimeException If PUT fails for any reason.
     */
    void putString(@Nonnull String source, @Nonnull Charset charset, @Nonnull URI relativeTarget);

    /**
     * PUTs the source string using UTF8 charset to target URI. The target MUST BE relative from the
     * {@link RemoteRepository#getUrl()} root.
     *
     * @throws RuntimeException If PUT fails for any reason.
     */
    default void putString(@Nonnull String source, @Nonnull URI relativeTarget) {
        putString(source, StandardCharsets.UTF_8, relativeTarget);
    }
}
