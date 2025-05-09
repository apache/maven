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

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.Constants;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.cli.extensions.CoreExtension;

import static java.util.Objects.requireNonNull;

/**
 * Represents the list of core extensions configured at one source. The list is validated (are GA unique), but no
 * other logic than that is applied.
 *
 * @since 4.0.0
 * @param source The source file of core extensions, is never {@code null}.
 * @param coreExtensions The configured core extensions, is never {@code null}. Contents of list is guaranteed to be unique by GA.
 *
 * @see Constants#MAVEN_PROJECT_EXTENSIONS
 * @see Constants#MAVEN_USER_EXTENSIONS
 * @see Constants#MAVEN_INSTALLATION_EXTENSIONS
 */
@Experimental
public record CoreExtensions(Path source, List<CoreExtension> coreExtensions) {
    public CoreExtensions(Path source, List<CoreExtension> coreExtensions) {
        this.source = requireNonNull(source, "source");
        this.coreExtensions = requireNonNull(coreExtensions, "coreExtensions");
    }
}
