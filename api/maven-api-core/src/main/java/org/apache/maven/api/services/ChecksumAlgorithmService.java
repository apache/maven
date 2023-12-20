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

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Checksum algorithms service.
 *
 * @since 4.0.0
 */
@Experimental
public interface ChecksumAlgorithmService extends Service {
    /**
     * Returns {@link ChecksumAlgorithm} for given algorithm name, or throws if algorithm not supported.
     *
     * @throws IllegalArgumentException if asked algorithm name is not supported.
     */
    @Nonnull
    ChecksumAlgorithm select(@Nonnull String algorithmName);

    /**
     * Returns a list of {@link ChecksumAlgorithm} in same order as algorithm names are ordered, or throws if any of the
     * algorithm name is not supported. The returned list has equal count of elements as passed in collection of names,
     * and if names contains duplicated elements, the returned list of algorithms will have duplicates as well.
     *
     * @throws IllegalArgumentException if any asked algorithm name is not supported.
     * @throws NullPointerException if passed in list of names is {@code null}.
     */
    @Nonnull
    List<ChecksumAlgorithm> selectList(@Nonnull Collection<String> algorithmNames);

    /**
     * Returns immutable collection of all supported algorithm names.
     */
    @Nonnull
    Collection<String> getChecksumAlgorithmNames();

    /**
     * Calculates checksums for specified data.
     *
     * @param data        The content for which to calculate checksums, must not be {@code null}.
     * @param algorithms  The checksum algorithm factories to use, must not be {@code null}.
     * @return The calculated checksums, indexed by algorithm name, or the exception that occurred while trying to
     * calculate it, never {@code null}.
     * @throws IOException In case of any problem.
     */
    @Nonnull
    Map<String, String> calculate(@Nonnull byte[] data, @Nonnull List<ChecksumAlgorithm> algorithms) throws IOException;

    /**
     * Calculates checksums for specified file.
     *
     * @param file        The file for which to calculate checksums, must not be {@code null}.
     * @param algorithms  The checksum algorithm factories to use, must not be {@code null}.
     * @return The calculated checksums, indexed by algorithm name, or the exception that occurred while trying to
     * calculate it, never {@code null}.
     * @throws IOException In case of any problem.
     */
    @Nonnull
    Map<String, String> calculate(@Nonnull Path file, @Nonnull List<ChecksumAlgorithm> algorithms) throws IOException;
}
