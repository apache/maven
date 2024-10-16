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

import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.v4.SettingsStaxWriter;

/**
 * Handles serialization of settings into the default textual format.
 *
 * @deprecated since 4.0.0, use {@link SettingsStaxWriter} instead
 */
@Named
@Singleton
@Deprecated(since = "4.0.0")
public class DefaultSettingsWriter implements SettingsWriter {

    @Override
    public void write(File output, Map<String, Object> options, Settings settings) throws IOException {
        Objects.requireNonNull(output, "output cannot be null");
        Objects.requireNonNull(settings, "settings cannot be null");

        output.getParentFile().mkdirs();

        write(Files.newOutputStream(output.toPath()), options, settings);
    }

    @Override
    public void write(Writer output, Map<String, Object> options, Settings settings) throws IOException {
        Objects.requireNonNull(output, "output cannot be null");
        Objects.requireNonNull(settings, "settings cannot be null");

        try (Writer out = output) {
            new SettingsStaxWriter().write(out, settings.getDelegate());
        } catch (XMLStreamException e) {
            throw new IOException("Error writing settings", e);
        }
    }

    @Override
    public void write(OutputStream output, Map<String, Object> options, Settings settings) throws IOException {
        Objects.requireNonNull(output, "output cannot be null");
        Objects.requireNonNull(settings, "settings cannot be null");

        try (OutputStream out = output) {
            new SettingsStaxWriter().write(out, settings.getDelegate());
        } catch (XMLStreamException e) {
            throw new IOException("Error writing settings", e);
        }
    }
}
