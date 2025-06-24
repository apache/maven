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
package org.apache.maven.api.services.model;

import java.nio.file.Path;

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Interface used to locate the root directory for a given project.
 *
 * The root locator is usually looked up from the DI container.
 * One notable exception is the computation of the early {@code session.rootDirectory}
 * property which happens very early.  The implementation used in this case
 * will be discovered using the JDK service mechanism.
 *
 * The default implementation will look for a {@code .mvn} child directory
 * or a {@code pom.xml} containing the {@code root="true"} attribute.
 */
public interface RootLocator extends Service {

    String UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE = "Unable to find the root directory. "
            + "Create a .mvn directory in the root directory or add the root=\"true\""
            + " attribute on the root project's model to identify it.";

    @Nullable
    Path findRoot(@Nonnull Path basedir);

    @Nonnull
    Path findMandatoryRoot(@Nonnull Path basedir);

    @Nonnull
    default String getNoRootMessage() {
        return UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE;
    }
}
