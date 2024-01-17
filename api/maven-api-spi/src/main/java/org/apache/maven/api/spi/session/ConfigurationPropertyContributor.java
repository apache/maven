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
package org.apache.maven.api.spi.session;

import java.util.Map;

import org.apache.maven.api.annotations.Experimental;

/**
 * Component able to contribute to Maven session configuration properties. This SPI component is invoked
 * very early, while there is no session created yet.
 */
@Experimental
public interface ConfigurationPropertyContributor {
    /**
     * Invoked just before session is created with a mutable map that carries collected configuration
     * properties so far.
     * <p>
     * <em>Note:</em> Configuration properties are of type {@code Map<String, Object>} and any key-value pair in the
     * input map that doesn't match this type will be silently ignored.
     *
     * @param configurationProperties The mutable configuration properties, never {@code null}.
     */
    void contribute(Map<?, ?> configurationProperties);
}
