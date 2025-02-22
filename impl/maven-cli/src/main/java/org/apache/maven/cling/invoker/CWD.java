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
package org.apache.maven.cling.invoker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.apache.maven.api.annotations.Nonnull;

import static java.util.Objects.requireNonNull;

/**
 * A thin wrapper for a {@link Path} that serves as "current working directory" value. Hence, this class
 * is mutable (as CWD may be changed), but allows transition only to existing directories.
 */
public final class CWD implements Supplier<Path> {
    /**
     * Creates instance out of {@link Path}.
     */
    public static CWD create(Path path) {
        return new CWD(Utils.getCanonicalPath(path));
    }

    private Path directory;

    private CWD(Path directory) {
        this.directory = directory;
    }

    @Nonnull
    @Override
    public Path get() {
        return directory;
    }

    /**
     * Resolves against current cwd, resulting path is normalized.
     *
     * @throws NullPointerException if {@code seg} is {@code null}.
     */
    @Nonnull
    public Path resolve(String seg) {
        requireNonNull(seg, "seg");
        return directory.resolve(seg).normalize();
    }

    /**
     * Changes current cwd, if the new path is existing directory.
     *
     * @throws NullPointerException if {@code seg} is {@code null}.
     * @throws IllegalArgumentException if {@code seg} leads to non-existent directory.
     */
    public void change(String seg) {
        Path newCwd = resolve(seg);
        if (Files.isDirectory(newCwd)) {
            this.directory = newCwd;
        } else {
            throw new IllegalArgumentException("Directory '" + directory + "' does not exist");
        }
    }
}
