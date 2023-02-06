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
package org.apache.maven.toolchain;

import java.io.File;

import org.apache.maven.toolchain.model.PersistedToolchains;

/**
 * Builds the toolchains model from a previously configured filesystem path to the toolchains file.
 * <strong>Note:</strong> This is an internal component whose interface can change without prior notice.
 *
 * @author Benjamin Bentmann
 */
public interface ToolchainsBuilder {

    /**
     * Builds the toolchains model from the configured toolchain files.
     *
     * @param userToolchainsFile The path to the toolchains file, may be <code>null</code> to disable parsing.
     * @return The toolchains model or <code>null</code> if no toolchain file was configured or the configured file does
     *         not exist.
     * @throws MisconfiguredToolchainException If the toolchain file exists but cannot be parsed.
     */
    PersistedToolchains build(File userToolchainsFile) throws MisconfiguredToolchainException;
}
