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
package org.apache.maven.model.root;

import javax.inject.Named;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class DefaultRootLocator implements RootLocator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Path findRoot(Path basedir) {
        Path rootDirectory = basedir;
        while (rootDirectory != null && !isRootDirectory(rootDirectory)) {
            rootDirectory = rootDirectory.getParent();
        }
        return rootDirectory;
    }

    @Override
    public Path findMandatoryRoot(Path basedir) {
        Path rootDirectory = findRoot(basedir);
        Optional<Path> rdf = getRootDirectoryFallback();
        if (rootDirectory == null) {
            rootDirectory = rdf.orElseThrow(() -> new IllegalStateException(getNoRootMessage()));
        } else {
            if (rdf.isPresent()) {
                try {
                    if (!Files.isSameFile(rootDirectory, rdf.get())) {
                        logger.warn("Project root directory and multiModuleProjectDirectory are not aligned");
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("findMandatoryRoot failed", e);
                }
            }
        }
        return rootDirectory;
    }

    protected Optional<Path> getRootDirectoryFallback() {
        String mmpd = System.getProperty("maven.multiModuleProjectDirectory");
        if (mmpd != null) {
            return Optional.of(getCanonicalPath(Paths.get(mmpd)));
        }
        return Optional.empty();
    }

    protected Path getCanonicalPath(Path path) {
        return path.toAbsolutePath().normalize();
    }

    public boolean isRootDirectory(Path dir) {
        return Files.isDirectory(dir.resolve(".mvn"));
    }
}
