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
package org.apache.maven.api.cli;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.cli.internal.extension.model.CoreExtension;

/**
 * Maven execution request.
 *
 * @param <O> the type of Options.
 */
public interface InvokerRequest<O extends Options> {
    /**
     * Mandatory: the current working directory, usually the {@code "user.dir"} Java System Property.
     */
    @Nonnull
    Path cwd();

    /**
     * Mandatory: the Maven installation directory, usually the {@code "maven.home"} Java System Property set by
     * Maven launcher script.
     */
    @Nonnull
    Path installationDirectory();

    /**
     * Mandatory: the user home directory, usually the {@code "user.home"} Java System Property.
     */
    @Nonnull
    Path userHomeDirectory();

    /**
     * Mandatory: Maven User Properties.
     */
    @Nonnull
    Map<String, String> userProperties();

    /**
     * Mandatory: Maven System Properties.
     */
    @Nonnull
    Map<String, String> systemProperties();

    /**
     * Mandatory: Logger to be used in early phases, until "real" Maven logger does not takes over.
     */
    @Nonnull
    Logger logger();

    /**
     * Mandatory: Maven Message builder factory.
     */
    @Nonnull
    MessageBuilderFactory messageBuilderFactory();

    /**
     * Derived: the top directory of invocation, directory where POM to execute resides.
     */
    @Nonnull
    Path topDirectory();

    /**
     * Derived: the root directory of invocation, directory where "root" was detected (by presence of {@code .mvn}
     * directory or having POM with {@code root="true"} property.
     * <p>
     * This field is nullable, but in fact should not be. Is nullable for Maven3 backward compatibility.
     */
    @Nullable
    Path rootDirectory();

    /**
     * Optional: if running embedded.
     */
    @Nonnull
    Optional<InputStream> in();

    /**
     * Optional: if running embedded.
     */
    @Nonnull
    Optional<OutputStream> out();

    /**
     * Optional: if running embedded.
     */
    @Nonnull
    Optional<OutputStream> err();

    /**
     * Optional: if core extensions were configured in {@code .mvn/extensions.xml} file.
     */
    @Nonnull
    Optional<List<CoreExtension>> coreExtensions();

    /**
     * The mandatory options.
     */
    @Nonnull
    O options();
}
