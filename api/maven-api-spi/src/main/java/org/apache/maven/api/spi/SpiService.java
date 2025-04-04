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

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;

/**
 * Marker interface to indicate services that can be provided by plugins and extensions.
 * <p>
 * This interface serves as the base for all Service Provider Interface (SPI) components in Maven.
 * Classes implementing this interface can be discovered and loaded by Maven through the
 * Java ServiceLoader mechanism, allowing plugins and extensions to contribute functionality
 * to the Maven build process.
 * <p>
 * SPI services are typically registered in {@code META-INF/services/} files corresponding to
 * the specific service interface being implemented.
 * <p>
 * All SPI services should be annotated with {@link Consumer} to indicate they are meant to be
 * implemented by plugins and extensions rather than used by them.
 *
 * @since 4.0.0
 */
@Experimental
@Consumer
public interface SpiService {}
