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
package org.apache.maven.logging.internal;

import java.util.List;
import java.util.Optional;

import org.apache.maven.api.services.OutputCapabilities.Destination;
import org.apache.maven.jline.FastTerminal;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminalProvider;
import org.jline.terminal.impl.exec.ExecTerminalProvider;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalExt;
import org.jline.terminal.spi.TerminalProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Internal terminal attachment detection for the CLI's system-output route. */
public final class TerminalOutputCapabilities {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminalOutputCapabilities.class);

    private TerminalOutputCapabilities() {}

    public static Optional<Destination> destination(Terminal terminal) {
        try {
            if (terminal instanceof FastTerminal fastTerminal) {
                LOGGER.debug("Resolving asynchronous terminal for logging output detection");
                terminal = fastTerminal.getTerminal();
            }
            if (!(terminal instanceof TerminalExt extended)) {
                LOGGER.debug(
                        "Logging destination is unavailable: terminal {} does not expose its provider and system stream",
                        terminal == null ? null : terminal.getClass().getName());
                return Optional.empty();
            }
            SystemStream stream = extended.getSystemStream();
            TerminalProvider provider = extended.getProvider();
            LOGGER.debug(
                    "Inspecting logging output: terminal={}, provider={}, stream={}",
                    terminal.getClass().getName(),
                    provider == null ? null : provider.getClass().getName(),
                    stream);
            if (provider != null && !(provider instanceof DumbTerminalProvider)) {
                // A non-system terminal may be backed by arbitrary embedder streams.
                if (stream == null) {
                    LOGGER.debug("Logging destination is unavailable: terminal provider has no system stream");
                    return Optional.empty();
                }
                return probe(provider, stream);
            }
            if (!(provider instanceof DumbTerminalProvider)) {
                LOGGER.debug("Logging destination is unavailable: terminal has no provider");
                return Optional.empty();
            }
            // A dumb terminal can also result from redirected stdin or failed terminal
            // creation. Ask the configured providers about output, not terminal type.
            SystemStream outputStream = stream != null ? stream : SystemStream.Output;
            String configuredProviders = System.getProperty(TerminalBuilder.PROP_PROVIDER);
            LOGGER.debug(
                    "Dumb terminal provider: probing configured providers {} for logging stream {}",
                    configuredProviders,
                    outputStream);
            IllegalStateException failure = new IllegalStateException("Unable to inspect terminal output");
            List<TerminalProvider> providers = TerminalBuilder.builder().getProviders(configuredProviders, failure);
            if (failure.getSuppressed().length > 0) {
                LOGGER.debug("Some terminal providers could not be loaded for logging output detection", failure);
            }
            return probe(providers, outputStream);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.debug("Logging destination is unavailable: terminal output detection failed", e);
            return Optional.empty();
        }
    }

    static Optional<Destination> probe(List<TerminalProvider> providers, SystemStream stream) {
        boolean redirected = false;
        for (TerminalProvider provider : providers) {
            if (provider instanceof DumbTerminalProvider) {
                LOGGER.debug("Skipping dumb terminal provider when probing logging stream {}", stream);
                continue;
            }
            Optional<Destination> result = probe(provider, stream);
            if (result.orElse(null) == Destination.CONSOLE) {
                return result;
            }
            redirected |= result.orElse(null) == Destination.REDIRECTED;
        }
        Optional<Destination> result = redirected ? Optional.of(Destination.REDIRECTED) : Optional.empty();
        LOGGER.debug("Logging destination for stream {} after probing configured providers: {}", stream, result);
        return result;
    }

    private static Optional<Destination> probe(TerminalProvider provider, SystemStream stream) {
        String providerClass = provider == null ? null : provider.getClass().getName();
        try {
            Optional<Destination> result;
            if (provider.isSystemStream(stream)) {
                result = Optional.of(Destination.CONSOLE);
            } else if (provider instanceof ExecTerminalProvider) {
                // The exec provider also returns false when its external probe fails.
                // A negative result cannot distinguish redirection from failed detection.
                LOGGER.debug(
                        "Logging destination is unavailable: exec terminal provider {} returned an inconclusive negative result for stream {}",
                        providerClass,
                        stream);
                return Optional.empty();
            } else {
                result = Optional.of(Destination.REDIRECTED);
            }
            LOGGER.debug("Terminal provider {} reports logging stream {} as {}", providerClass, stream, result);
            return result;
        } catch (RuntimeException | LinkageError e) {
            LOGGER.debug(
                    "Logging destination is unavailable: terminal provider {} failed to inspect stream {}",
                    providerClass,
                    stream,
                    e);
            return Optional.empty();
        }
    }
}
