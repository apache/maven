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
package org.apache.maven.api.cli.mvn.forked;

import java.util.List;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.mvn.MavenInvokerRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;

/**
 * Represents a request to invoke Maven in a forked JVM.
 * This interface extends the {@link MavenInvokerRequest}, adding capabilities specific to forked Maven executions.
 *
 * @since 4.0.0
 */
@Experimental
public interface ForkedMavenInvokerRequest extends MavenInvokerRequest<MavenOptions> {
    /**
     * Returns the list of extra JVM arguments to be passed to the forked Maven process.
     * These arguments allow for customization of the JVM environment in which Maven will run.
     *
     * @return an Optional containing the list of extra JVM arguments, or empty if not specified
     */
    @Nonnull
    Optional<List<String>> jvmArguments();
}
