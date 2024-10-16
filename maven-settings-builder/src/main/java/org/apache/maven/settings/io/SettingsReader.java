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
package org.apache.maven.settings.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.apache.maven.settings.Settings;

/**
 * Handles deserialization of settings from some kind of textual format like XML.
 *
 * @deprecated since 4.0.0, use {@link org.apache.maven.api.services.xml.SettingsXmlFactory} instead
 */
@Deprecated(since = "4.0.0")
public interface SettingsReader {

    /**
     * The key for the option to enable strict parsing. This option is of type {@link Boolean} and defaults to {@code
     * true}. If {@code false}, unknown elements will be ignored instead of causing a failure.
     */
    String IS_STRICT = "org.apache.maven.settings.io.isStrict";

    /**
     * Reads the settings from the specified file.
     *
     * @param input The file to deserialize the settings from, must not be {@code null}.
     * @param options The options to use for deserialization, may be {@code null} to use the default values.
     * @return The deserialized settings, never {@code null}.
     * @throws IOException If the settings could not be deserialized.
     * @throws SettingsParseException If the input format could not be parsed.
     */
    Settings read(File input, Map<String, ?> options) throws IOException, SettingsParseException;

    /**
     * Reads the settings from the specified character reader. The reader will be automatically closed before the method
     * returns.
     *
     * @param input The reader to deserialize the settings from, must not be {@code null}.
     * @param options The options to use for deserialization, may be {@code null} to use the default values.
     * @return The deserialized settings, never {@code null}.
     * @throws IOException If the settings could not be deserialized.
     * @throws SettingsParseException If the input format could not be parsed.
     */
    Settings read(Reader input, Map<String, ?> options) throws IOException, SettingsParseException;

    /**
     * Reads the settings from the specified byte stream. The stream will be automatically closed before the method
     * returns.
     *
     * @param input The stream to deserialize the settings from, must not be {@code null}.
     * @param options The options to use for deserialization, may be {@code null} to use the default values.
     * @return The deserialized settings, never {@code null}.
     * @throws IOException If the settings could not be deserialized.
     * @throws SettingsParseException If the input format could not be parsed.
     */
    Settings read(InputStream input, Map<String, ?> options) throws IOException, SettingsParseException;
}
