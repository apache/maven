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
package org.apache.maven.settings.building;

import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.apache.maven.settings.io.SettingsReader;
import org.apache.maven.settings.io.SettingsWriter;
import org.apache.maven.settings.validation.DefaultSettingsValidator;
import org.apache.maven.settings.validation.SettingsValidator;

/**
 * A factory to create settings builder instances when no dependency injection is available. <em>Note:</em> This class
 * is only meant as a utility for developers that want to employ the settings builder outside of the Maven build system,
 * Maven plugins should always acquire settings builder instances via dependency injection. Developers might want to
 * subclass this factory to provide custom implementations for some of the components used by the settings builder.
 *
 * @author Benjamin Bentmann
 */
public class DefaultSettingsBuilderFactory {

    protected SettingsReader newSettingsReader() {
        return new DefaultSettingsReader();
    }

    protected SettingsWriter newSettingsWriter() {
        return new DefaultSettingsWriter();
    }

    protected SettingsValidator newSettingsValidator() {
        return new DefaultSettingsValidator();
    }

    /**
     * Creates a new settings builder instance.
     *
     * @return The new settings builder instance, never {@code null}.
     */
    public DefaultSettingsBuilder newInstance() {
        return new DefaultSettingsBuilder(newSettingsReader(), newSettingsWriter(), newSettingsValidator());
    }
}
