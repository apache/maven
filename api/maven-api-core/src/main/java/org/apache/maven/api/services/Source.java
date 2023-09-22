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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Provides access to the contents of a source independently of the
 * backing store (e.g. file system, database, memory).
 * <p>
 * This is mainly used to parse files into objects such as
 * {@link org.apache.maven.api.Project},
 * {@link org.apache.maven.api.model.Model},
 * {@link org.apache.maven.api.settings.Settings}, or
 * {@link org.apache.maven.api.toolchain.PersistedToolchains}.
 *
 * @since 4.0.0
 * @see org.apache.maven.api.services.ProjectBuilder#build(Session, Source)
 * @see org.apache.maven.api.services.SettingsBuilder#build(Session, Source, Source, Source)
 * @see org.apache.maven.api.services.ToolchainsBuilder#build(Session, Source, Source)
 */
@Experimental
public interface Source {

    /**
     * Provides access the file to be parsed, if this source is backed by a file.
     *
     * @return the underlying {@code Path}, or {@code null} if this source is not backed by a file
     */
    @Nullable
    Path getPath();

    /**
     * Creates a new byte stream to the source contents.
     * Closing the returned stream is the responsibility of the caller.
     *
     * @return a byte stream to the source contents, never {@code null}
     * @throws IOException in case of IO issue
     */
    @Nonnull
    InputStream openStream() throws IOException;

    /**
     * Provides a user-friendly hint about the location of the source.
     * This could be a local file path, a URI or just an empty string.
     * The intention is to assist users during error reporting.
     *
     * @return a user-friendly hint about the location of the source, never {@code null}
     */
    @Nonnull
    String getLocation();

    /**
     * Returns a new source identified by a relative path. Implementation <strong>MUST</strong>
     * be able to accept <code>relative</code> parameter values that
     * <ul>
     * <li>use either / or \ file path separator,</li>
     * <li>have .. parent directory references,</li>
     * <li>point either at file or directory.</li>
     * </ul>
     *
     * @param relative is the path of the requested source relative to this source
     * @return related source or <code>null</code> if no such source
     */
    Source resolve(String relative);
}
