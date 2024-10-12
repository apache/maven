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

import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.ThreadSafe;
import org.apache.maven.api.build.Status;

/**
 * {@code Workspace} provides a layer of indirection between BuildContext and underlying resource
 * (File) store.
 */
@Experimental
@ThreadSafe
@Consumer
public interface Workspace {

    Mode getMode();

    Workspace escalate();

    boolean isPresent(Path file);

    boolean isRegularFile(Path file);

    boolean isDirectory(Path file);

    /**
     * @throws org.apache.maven.api.build.BuildContextException
     */
    void deleteFile(Path file);

    /**
     * Indicates to the workspace that this output path has been processed.
     *
     * @param path the output path.
     */
    void processOutput(Path path);

    /**
     * Returns an output stream on the specified file.
     * The workspace may want to optimize using a caching stream which will
     * only overwrite the target file if the content is actually changed.
     *
     * @return an output stream
     * @throws org.apache.maven.api.build.BuildContextException
     */
    OutputStream newOutputStream(Path path);

    Status getResourceStatus(Path file, FileTime lastModified, long size);

    /**
     * Walks a file tree.
     * <p>
     * Files visited and their status depends on workspace mode.
     * <ul>
     * <li><strong>{@code NORMAL}</strong> all files are visited and all file status is reported as
     * NEW. BuildContext is expected to calculate actual input resource status.</li>
     * <li><strong>{@code DELTA}</strong> only NEW, MODIFIED or REMOVED files are
     * visited.</li>
     * <li><strong>{@code ESCALATED}</strong> all files are visited and all file status is reported as
     * NEW. This mode is used when the user has explicitly requested full build in IDE. BuildContext
     * must treat all files as either NEW or MODIFIED.</li>
     * <li><strong>{@code SUPPRESSED}</strong> This mode is used during so-called "configuration"
     * build, when all inputs are assumed up-to-date, no outputs are expected to be created, updated
     * or removed. The idea is to allow host application to collect build configuration information
     * (compile source roots, properties, etc) without doing the actual build.</li>
     * </ul>
     *
     * @throws org.apache.maven.api.build.BuildContextException
     */
    Stream<FileState> walk(Path basedir);

    enum Mode {
        NORMAL,
        DELTA,
        ESCALATED,
        SUPPRESSED
    }
}
