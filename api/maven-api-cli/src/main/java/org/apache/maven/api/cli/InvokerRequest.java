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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.cisupport.CIInfo;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.MessageBuilderFactory;

/**
 * Represents a Maven invocation request, encapsulating all necessary information
 * for invoking a Maven build or command. Arguments are parsed and exposed via methods.
 *
 * @since 4.0.0
 */
@Immutable
@Experimental
public interface InvokerRequest {
    /**
     * The parser request this instance was created from.
     */
    @Nonnull
    ParserRequest parserRequest();

    /**
     * Flag representing parser processing result: if there were some fatal errors during
     * {@link Parser#parseInvocation(ParserRequest)} this method will return {@code true} and invoker should
     * handle this request as "early failure".
     */
    boolean parsingFailed();

    /**
     * Returns {@code true} if this call happens in "embedded" mode.
     *
     * @see ParserRequest#embedded()
     */
    default boolean embedded() {
        return parserRequest().embedded();
    }

    /**
     * Returns the current working directory for the Maven execution.
     * This is typically the directory from which Maven was invoked.
     *
     * @return the current working directory path
     */
    @Nonnull
    Path cwd();

    /**
     * Returns the Maven installation directory.
     * This is usually set by the Maven launcher script using the "maven.home" system property.
     *
     * @return the Maven installation directory path
     */
    @Nonnull
    Path installationDirectory();

    /**
     * Returns the user's home directory.
     * This is typically obtained from the "user.home" system property.
     *
     * @return the user's home directory path
     */
    @Nonnull
    Path userHomeDirectory();

    /**
     * Shorthand for {@link MessageBuilderFactory}.
     */
    default MessageBuilderFactory messageBuilderFactory() {
        return parserRequest().messageBuilderFactory();
    }

    /**
     * Shorthand for {@link Lookup}.
     */
    default Lookup lookup() {
        return parserRequest().lookup();
    }

    /**
     * Returns a map of user-defined properties for the Maven execution.
     * These properties can be set using the -D command-line option.
     *
     * @return an unmodifiable map of user properties
     */
    @Nonnull
    Map<String, String> userProperties();

    /**
     * Returns a map of system properties for the Maven execution.
     * These include both Java system properties and Maven-specific system properties.
     *
     * @return an unmodifiable map of system properties
     */
    @Nonnull
    Map<String, String> systemProperties();

    /**
     * Returns the top-level directory of the Maven invocation.
     * This is typically the directory containing the POM file being executed.
     *
     * @return the top-level directory path
     */
    @Nonnull
    Path topDirectory();

    /**
     * Returns the root directory of the Maven invocation, if found. This is determined by the presence of a
     * {@code .mvn} directory or a POM with the root="true" property but is not always applicable (ie invocation
     * from outside a checkout).
     *
     * @return the root directory path, if present
     */
    @Nonnull
    Optional<Path> rootDirectory();

    /**
     * Returns the input stream for the Maven execution, if running in embedded mode.
     *
     * @return an {@link Optional} containing the input stream, or empty if not applicable
     */
    @Nonnull
    default Optional<InputStream> stdIn() {
        return Optional.ofNullable(parserRequest().stdIn());
    }

    /**
     * Returns the output stream for the Maven execution, if running in embedded mode.
     *
     * @return an {@link Optional} containing the output stream, or empty if not applicable
     */
    @Nonnull
    default Optional<OutputStream> stdOut() {
        return Optional.ofNullable(parserRequest().stdOut());
    }

    /**
     * Returns the error stream for the Maven execution, if running in embedded mode.
     *
     * @return an {@link Optional} containing the error stream, or empty if not applicable
     */
    @Nonnull
    default Optional<OutputStream> stdErr() {
        return Optional.ofNullable(parserRequest().stdErr());
    }

    /**
     * Returns a list of core extensions from all sources, that were discovered and loaded. Each instance of
     * {@link CoreExtensions} is validated, but the list elements may have overlapping elements, that requires
     * some logic to sort out (like precedence).
     * <p>
     * The list of {@link CoreExtensions} if present, is in precedence order.
     *
     * @return an {@link Optional} containing the {@link CoreExtensions}, or empty if not configured
     */
    @Nonnull
    Optional<List<CoreExtensions>> coreExtensions();

    /**
     * Returns detected CI system, if any.
     *
     * @return an {@link Optional} containing the {@link CIInfo} collected from CI system. or empty if CI not
     * detected.
     */
    @Nonnull
    Optional<CIInfo> ciInfo();

    /**
     * Returns the options associated with this invocation request.
     *
     * @return the options object
     */
    @Nonnull
    Options options();

    /**
     * This method returns "verbose" option value derived from multiple places: CLI options, but also CI detection,
     * if applicable.
     */
    default boolean effectiveVerbose() {
        return options().verbose().orElse(ciInfo().isPresent() && ciInfo().get().isVerbose());
    }
}
