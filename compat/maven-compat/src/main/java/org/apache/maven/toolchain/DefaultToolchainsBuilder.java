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

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.v4.MavenToolchainsStaxReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated instead use {@link org.apache.maven.toolchain.building.DefaultToolchainsBuilder}
 */
@Deprecated
@Named("default")
@Singleton
public class DefaultToolchainsBuilder implements ToolchainsBuilder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PersistedToolchains build(File userToolchainsFile) throws MisconfiguredToolchainException {
        PersistedToolchains toolchains = null;

        if (userToolchainsFile != null && userToolchainsFile.isFile()) {
            try (InputStream in = Files.newInputStream(userToolchainsFile.toPath())) {
                toolchains = new PersistedToolchains(new MavenToolchainsStaxReader().read(in));
            } catch (Exception e) {
                throw new MisconfiguredToolchainException(
                        "Cannot read toolchains file at " + userToolchainsFile.getAbsolutePath(), e);
            }

        } else if (userToolchainsFile != null) {
            logger.debug("Toolchains configuration was not found at {}", userToolchainsFile);
        }

        return toolchains;
    }
}
