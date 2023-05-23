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
package org.apache.maven.api.build.spi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.apache.maven.api.build.BuildContextException;
import org.apache.maven.api.build.Status;

public final class FileState {
    private final Path path;
    private final FileTime lastModified;
    private final long size;
    private final Status status;

    public FileState(Path path, FileTime lastModified, long size, Status status) {
        this.path = path;
        this.lastModified = lastModified;
        this.size = size;
        this.status = status;
    }

    public FileState(Path path, Status status) {
        this.path = path;
        this.status = status;
        if (status == Status.REMOVED) {
            lastModified = null;
            size = 0;
        } else {
            try {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                this.lastModified = attrs.lastModifiedTime();
                this.size = attrs.size();
            } catch (IOException e) {
                throw new BuildContextException(e);
            }
        }
    }

    public Path getPath() {
        return path;
    }

    public FileTime getLastModified() {
        return lastModified;
    }

    public long getSize() {
        return size;
    }

    public Status getStatus() {
        return status;
    }
}
