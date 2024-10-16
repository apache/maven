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
package org.apache.maven.toolchain.io;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.api.toolchain.InputSource;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.v4.MavenToolchainsStaxReader;

/**
 * Handles deserialization of toolchains from the default textual format.
 *
 * @since 3.3.0
 * @deprecated since 4.0.0, use {@link MavenToolchainsStaxReader} instead
 */
@Named
@Singleton
@Deprecated(since = "4.0.0")
public class DefaultToolchainsReader implements ToolchainsReader {

    @Override
    public PersistedToolchains read(File input, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(input, "input cannot be null");

        try (InputStream in = Files.newInputStream(input.toPath())) {
            InputSource source = new InputSource(input.toString());
            return new PersistedToolchains(new MavenToolchainsStaxReader().read(in, isStrict(options), source));
        } catch (XMLStreamException e) {
            throw new ToolchainsParseException(
                    e.getMessage(),
                    e.getLocation().getLineNumber(),
                    e.getLocation().getColumnNumber(),
                    e);
        }
    }

    @Override
    public PersistedToolchains read(Reader input, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(input, "input cannot be null");

        try (Reader in = input) {
            InputSource source = (InputSource) options.get(InputSource.class.getName());
            return new PersistedToolchains(new MavenToolchainsStaxReader().read(in, isStrict(options), source));
        } catch (XMLStreamException e) {
            throw new ToolchainsParseException(
                    e.getMessage(),
                    e.getLocation().getLineNumber(),
                    e.getLocation().getColumnNumber(),
                    e);
        }
    }

    @Override
    public PersistedToolchains read(InputStream input, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(input, "input cannot be null");

        try (InputStream in = input) {
            InputSource source = (InputSource) options.get(InputSource.class.getName());
            return new PersistedToolchains(new MavenToolchainsStaxReader().read(in, isStrict(options), source));
        } catch (XMLStreamException e) {
            throw new ToolchainsParseException(
                    e.getMessage(),
                    e.getLocation().getLineNumber(),
                    e.getLocation().getColumnNumber(),
                    e);
        }
    }

    private boolean isStrict(Map<String, ?> options) {
        Object value = (options != null) ? options.get(IS_STRICT) : null;
        return value == null || Boolean.parseBoolean(value.toString());
    }
}
