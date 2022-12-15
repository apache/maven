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
package org.apache.maven.configuration;

/**
 * Unmarshals some textual configuration from the POM or similar into the properties of a bean. This component works
 * similar to the way Maven configures plugins from the POM, i.e. some configuration like {@code <param>value</param>}
 * is mapped to an equally named property of the bean and converted. The properties of the bean are supposed to either
 * have a public setter or be backed by an equally named field (of any visibility).
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
public interface BeanConfigurator {

    /**
     * Performs the specified bean configuration.
     *
     * @param request The configuration request that specifies the bean and the configuration to process, must not be
     *            {@code null}.
     * @throws BeanConfigurationException If the bean configuration could not be successfully processed.
     */
    void configureBean(BeanConfigurationRequest request) throws BeanConfigurationException;
}
