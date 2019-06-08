package org.apache.maven.toolchain.io;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.toolchain.model.PersistedToolchains;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Handles serialization of toolchains into some kind of textual format like XML.
 *
 * @author Mike Mol
 * @author Martin Kanters
 */
public interface ToolchainsWriter
{

    /**
     * Writes the supplied toolchains to the specified character writer. The writer will be automatically closed before
     * the method returns.
     *
     * @param output The writer to serialize the toolchains to, must not be {@code null}.
     * @param options The options to use for serialization, may be {@code null} to use the default values.
     * @param toolchains The toolchains to serialize, must not be {@code null}.
     * @throws IOException If the toolchains could not be serialized.
     */
    void write( Writer output, Map<String, Object> options, PersistedToolchains toolchains )
            throws IOException;
}
