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
package org.apache.maven.cling.invoker.mvn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static java.util.Objects.requireNonNull;

/**
 * Class that sets up Mimir for maven-cli tests IF outer build uses Mimir as well (CI setup).
 */
public final class MimirInfuser {
    public static void infuse(Path userHome) throws IOException {
        requireNonNull(userHome);
        // GH CI copies this to place, or user may have it already
        Path realUserWideExtensions =
                Path.of(System.getProperty("user.home")).resolve(".m2").resolve("extensions.xml");
        if (Files.isRegularFile(realUserWideExtensions)) {
            String realUserWideExtensionsString = Files.readString(realUserWideExtensions);
            if (realUserWideExtensionsString.contains("<groupId>eu.maveniverse.maven.mimir</groupId>")
                    && realUserWideExtensionsString.contains("<artifactId>extension</artifactId>")) {
                Path userWideExtensions = userHome.resolve(".m2").resolve("extensions.xml");
                // some tests do prepare project and user wide extensions; skip those for now
                if (!Files.isRegularFile(userWideExtensions)) {
                    Files.createDirectories(userWideExtensions.getParent());
                    Files.copy(realUserWideExtensions, userWideExtensions, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
