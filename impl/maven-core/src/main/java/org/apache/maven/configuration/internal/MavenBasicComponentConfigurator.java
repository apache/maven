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
package org.apache.maven.configuration.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.eclipse.sisu.Priority;

/**
 * A {@link BasicComponentConfigurator} for the {@code basic} role-hint that uses
 * {@link EnhancedStringConverter} to properly handle empty configuration tags for Maven 3 plugins.
 */
@Singleton
@Named("basic")
@Priority(10)
public class MavenBasicComponentConfigurator extends BasicComponentConfigurator {

    public MavenBasicComponentConfigurator() {
        converterLookup.registerConverter(new EnhancedStringConverter());
    }
}
