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
package org.apache.maven.impl.model.rootlocator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.services.model.RootDetector;
import org.apache.maven.api.services.model.RootLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

@Named
public class DefaultRootLocator implements RootLocator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<RootDetector> rootDetectors;

    public DefaultRootLocator() {
        this.rootDetectors = ServiceLoader.load(RootDetector.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }

    @Override
    public Path findRoot(@Nonnull Path basedir) {
        requireNonNull(basedir, getNoRootMessage());
        Path rootDirectory = basedir;
        while (rootDirectory != null && !isRootDirectory(rootDirectory)) {
            rootDirectory = rootDirectory.getParent();
        }
        return rootDirectory;
    }

    @Nonnull
    public Path findMandatoryRoot(@Nonnull Path basedir) {
        try {
            Path rootDirectory = findRoot(basedir);
            Optional<Path> rdf = getRootDirectoryFallback();
            if (rootDirectory == null) {
                rootDirectory = rdf.orElseThrow(() -> new IllegalStateException(getNoRootMessage()));
                logger.warn(getNoRootMessage());
            } else {
                if (rdf.isPresent() && !Files.isSameFile(rootDirectory, rdf.get())) {
                    logger.warn("Project root directory and multiModuleProjectDirectory are not aligned");
                }
            }
            return rootDirectory;
        } catch (IOException e) {
            throw new UncheckedIOException("findMandatoryRoot failed", e);
        }
    }

    protected boolean isRootDirectory(Path dir) {
        requireNonNull(dir, "dir is null");
        for (RootDetector rootDetector : rootDetectors) {
            if (rootDetector.isRootDirectory(dir)) {
                return true;
            }
        }
        return false;
    }

    protected Optional<Path> getRootDirectoryFallback() throws IOException {
        String mmpd = System.getProperty("maven.multiModuleProjectDirectory");
        if (mmpd != null) {
            return Optional.of(getCanonicalPath(Paths.get(mmpd)));
        }
        return Optional.empty();
    }

    protected Path getCanonicalPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return getCanonicalPath(path.getParent()).resolve(path.getFileName());
        }
    }
}
