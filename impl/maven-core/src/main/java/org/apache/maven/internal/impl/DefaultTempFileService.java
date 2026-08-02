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
package org.apache.maven.internal.impl;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.services.TempFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.maven.api.Constants.KEEP_PROP;

/**
 * Default TempFileService implementation.
 * Stores tracked paths in Session-scoped data and removes them after the build.
 */
@Named
@Singleton
public final class DefaultTempFileService implements TempFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTempFileService.class);

    // unique, typed session key (uses factory; one narrow unchecked cast)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final SessionData.Key<Set<Path>> TMP_KEY =
            (SessionData.Key) SessionData.key(Set.class, DefaultTempFileService.class);

    // supplier with concrete types (avoids inference noise)
    private static final Supplier<Set<Path>> TMP_SUPPLIER =
            () -> Collections.newSetFromMap(new ConcurrentHashMap<Path, Boolean>());

    @Override
    public Path createTempFile(final Session session, final String prefix, final String suffix) throws IOException {
        Objects.requireNonNull(session, "session");
        final Path file = Files.createTempFile(prefix, suffix);
        register(session, file);
        return file;
    }

    @Override
    public Path createTempFile(final Session session, final String prefix, final String suffix, final Path directory)
            throws IOException {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(directory, "directory");
        final Path file = Files.createTempFile(directory, prefix, suffix);
        register(session, file);
        return file;
    }

    @Override
    public Path createTempDirectory(final Session session, final String prefix) throws IOException {
        Objects.requireNonNull(session, "session");
        final Path dir = Files.createTempDirectory(prefix);
        register(session, dir);
        return dir;
    }

    @Override
    public Path createTempDirectory(final Session session, final String prefix, final Path directory)
            throws IOException {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(directory, "directory");
        final Path dir = Files.createTempDirectory(directory, prefix);
        register(session, dir);
        return dir;
    }

    @Override
    public void register(final Session session, final Path path) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(path, "path");
        final Set<Path> bucket = sessionPaths(session);
        bucket.add(path);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Temp path registered for cleanup: {}", path);
        }
    }

    @Override
    public void cleanup(final Session session) throws IOException {
        Objects.requireNonNull(session, "session");

        if (Boolean.getBoolean(KEEP_PROP)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Skipping temp cleanup due to -D{}=true", KEEP_PROP);
            }
            return;
        }

        final Set<Path> bucket = sessionPaths(session);
        IOException first = null;

        for (final Path path : bucket) {
            try {
                deleteTree(path);
            } catch (final IOException e) {
                if (first == null) {
                    first = e;
                } else if (e != first) {
                    first.addSuppressed(e);
                }
                LOGGER.warn("Failed to delete temp path {}", path, e);
            }
        }
        bucket.clear();

        if (first != null) {
            throw first;
        }
    }

    // ---- internals ---------------------------------------------------------

    private Set<Path> sessionPaths(final Session session) {
        return session.getData().computeIfAbsent(TMP_KEY, TMP_SUPPLIER);
    }

    private static void deleteTree(final Path path) throws IOException {
        if (path == null || Files.notExists(path)) {
            return;
        }
        // Walk depth-first and delete files, then directories.
        Files.walkFileTree(
                path, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                            throws IOException {
                        Files.deleteIfExists(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                            throws IOException {
                        Files.deleteIfExists(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }
}
