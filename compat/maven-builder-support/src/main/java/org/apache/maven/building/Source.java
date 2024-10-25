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
package org.apache.maven.building;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides access to the contents of a source independently of the backing store (e.g. file system, database, memory).
 *
 */
public interface Source {

    /**
     * Gets a byte stream to the source contents. Closing the returned stream is the responsibility of the caller.
     *
     * @return A byte stream to the source contents, never {@code null}.
     * @throws IOException in case of IO issue
     */
    InputStream getInputStream() throws IOException;

    /**
     * Provides a user-friendly hint about the location of the source. This could be a local file path, a URI or just an
     * empty string. The intention is to assist users during error reporting.
     *
     * @return A user-friendly hint about the location of the source, never {@code null}.
     */
    String getLocation();
}
