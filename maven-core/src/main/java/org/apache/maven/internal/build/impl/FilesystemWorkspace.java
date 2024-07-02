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
package org.apache.maven.internal.build.impl;

import javax.inject.Named;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.maven.api.build.BuildContextException;
import org.apache.maven.api.build.Status;
import org.apache.maven.api.build.spi.FileState;
import org.apache.maven.api.build.spi.Workspace;
import org.codehaus.plexus.util.io.CachingOutputStream;

@Named
public class FilesystemWorkspace implements Workspace {

    @Override
    public Mode getMode() {
        return Mode.NORMAL;
    }

    @Override
    public Workspace escalate() {
        return this;
    }

    @Override
    public void deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new BuildContextException(e);
        }
    }

    @Override
    public void processOutput(Path path) {}

    @Override
    public OutputStream newOutputStream(Path path) {
        try {
            Files.createDirectories(path.getParent());
            return new CachingOutputStream(path);
        } catch (IOException e) {
            throw new BuildContextException(e);
        }
    }

    @Override
    public Status getResourceStatus(Path file, FileTime lastModified, long length) {
        if (!isRegularFile(file) && !isDirectory(file)) {
            return Status.REMOVED;
        }
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            return Objects.equals(length, attrs.size()) && Objects.equals(lastModified, attrs.lastModifiedTime())
                    ? Status.UNMODIFIED
                    : Status.MODIFIED;
        } catch (IOException e) {
            throw new BuildContextException(e);
        }
    }

    @Override
    public boolean isPresent(Path file) {
        return isRegularFile(file) && Files.isReadable(file);
    }

    @Override
    public boolean isRegularFile(Path file) {
        return Files.isRegularFile(file);
    }

    @Override
    public boolean isDirectory(Path file) {
        return Files.isDirectory(file);
    }

    @Override
    public Stream<org.apache.maven.api.build.spi.FileState> walk(Path basedir) {
        if (Files.isDirectory(basedir)) {
            try {
                return Files.walk(basedir).map(path -> new FileState(path, Status.NEW));
            } catch (IOException e) {
                throw new BuildContextException(e);
            }
        } else {
            return Stream.empty();
        }
    }
}
