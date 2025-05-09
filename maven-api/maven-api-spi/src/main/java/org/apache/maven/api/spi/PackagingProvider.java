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
package org.apache.maven.api.spi;

import org.apache.maven.api.Packaging;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.di.Named;

/**
 * Service provider interface for registering custom {@link Packaging} implementations.
 * <p>
 * This interface allows plugins and extensions to define and register additional packaging types
 * beyond the standard ones provided by Maven (like jar, war, ear, etc.). Implementations of this
 * interface will be discovered through the Java ServiceLoader mechanism and their provided
 * packaging types will be available throughout the Maven build process.
 * <p>
 * Example usage:
 * <pre>
 * public class CustomPackagingProvider implements PackagingProvider {
 *     public Collection&lt;Packaging&gt; provides() {
 *         return Arrays.asList(
 *             packaging("docker-image"),
 *             packaging("flatpak")
 *         );
 *     }
 * }
 * </pre>
 *
 * @see org.apache.maven.api.Packaging
 * @see org.apache.maven.api.spi.ExtensibleEnumProvider
 * @since 4.0.0
 */
@Experimental
@Consumer
@Named
public interface PackagingProvider extends ExtensibleEnumProvider<Packaging> {}
