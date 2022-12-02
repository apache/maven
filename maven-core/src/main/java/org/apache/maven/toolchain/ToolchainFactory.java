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

import org.apache.maven.toolchain.model.ToolchainModel;

/**
 * Internal toolchain factory, to prepare toolchains instances.
 *
 * @author mkleint
 * @since 2.0.9
 */
public interface ToolchainFactory {
    /**
     * Create instance of toolchain.
     **/
    ToolchainPrivate createToolchain(ToolchainModel model) throws MisconfiguredToolchainException;

    /**
     * Returns the default instance of the particular type of toolchain, can return <code>null</code>
     * if not applicable.
     * TODO keep around??
     **/
    ToolchainPrivate createDefaultToolchain();
}
