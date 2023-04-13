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
package org.apache.maven.plugin.internal;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Service responsible for validating plugin configuration.
 *
 * @author Slawomir Jaranowski
 */
interface MavenPluginConfigurationValidator {
    /**
     * Checks mojo configuration issues.
     */
    void validate(
            MavenSession mavenSession,
            MojoDescriptor mojoDescriptor,
            Class<?> mojoClass,
            PlexusConfiguration pomConfiguration,
            ExpressionEvaluator expressionEvaluator);
}
