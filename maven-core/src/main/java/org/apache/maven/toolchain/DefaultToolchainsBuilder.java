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
import java.io.Reader;

import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.io.xpp3.MavenToolchainsXpp3Reader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * @author Benjamin Bentmann
 * @deprecated instead use {@link org.apache.maven.toolchain.building.DefaultToolchainsBuilder}
 */
@Deprecated
@Component(role = ToolchainsBuilder.class, hint = "default")
public class DefaultToolchainsBuilder implements ToolchainsBuilder {

    @Requirement
    private Logger logger;

    public PersistedToolchains build(File userToolchainsFile) throws MisconfiguredToolchainException {
        PersistedToolchains toolchains = null;

        if (userToolchainsFile != null && userToolchainsFile.isFile()) {
            try (Reader in = ReaderFactory.newXmlReader(userToolchainsFile)) {
                toolchains = new MavenToolchainsXpp3Reader().read(in);
            } catch (Exception e) {
                throw new MisconfiguredToolchainException(
                        "Cannot read toolchains file at " + userToolchainsFile.getAbsolutePath(), e);
            }

        } else if (userToolchainsFile != null) {
            logger.debug("Toolchains configuration was not found at " + userToolchainsFile);
        }

        return toolchains;
    }
}
