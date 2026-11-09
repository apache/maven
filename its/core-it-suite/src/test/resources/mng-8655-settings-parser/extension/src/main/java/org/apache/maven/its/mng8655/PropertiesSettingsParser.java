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
package org.apache.maven.its.mng8655;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.settings.Profile;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.spi.SettingsParser;

@Named("properties")
@Singleton
public class PropertiesSettingsParser implements SettingsParser {
    @Inject
    public PropertiesSettingsParser() {}

    @Override
    public boolean supports(Source source) {
        return source.getLocation().endsWith(".properties");
    }

    @Override
    public Settings parse(Source source, Map<String, ?> options) throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = source.openStream()) {
            properties.load(stream);
        }
        return Settings.newBuilder()
                .profiles(List.of(Profile.newBuilder()
                        .id("custom-settings")
                        .properties(Map.of("parser.value", properties.getProperty("value")))
                        .build()))
                .activeProfiles(List.of("custom-settings"))
                .build();
    }
}
