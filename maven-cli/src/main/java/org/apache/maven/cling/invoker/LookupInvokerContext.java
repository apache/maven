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
package org.apache.maven.cling.invoker;

import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.function.Function;

import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Logger;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.cli.logging.Slf4jConfiguration;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuilder;
import org.slf4j.ILoggerFactory;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("VisibilityModifier")
public class LookupInvokerContext<
                O extends Options, R extends InvokerRequest<O>, C extends LookupInvokerContext<O, R, C>>
        implements AutoCloseable {
    public final LookupInvoker<O, R, C> lookupInvoker;
    public final ProtoLookup protoLookup;
    public final R invokerRequest;
    public final Function<String, Path> cwdResolver;
    public final InputStream stdIn;
    public final PrintWriter stdOut;
    public final PrintWriter stdErr;

    protected LookupInvokerContext(LookupInvoker<O, R, C> lookupInvoker, R invokerRequest) {
        this.lookupInvoker = lookupInvoker;
        this.protoLookup = lookupInvoker.protoLookup;
        this.invokerRequest = requireNonNull(invokerRequest);
        this.cwdResolver = s -> invokerRequest.cwd().resolve(s).normalize().toAbsolutePath();
        this.stdIn = invokerRequest.in().orElse(System.in);
        this.stdOut = new PrintWriter(invokerRequest.out().orElse(System.out), true);
        this.stdErr = new PrintWriter(invokerRequest.err().orElse(System.err), true);
        this.logger = invokerRequest.logger();
    }

    public Logger logger;
    public ILoggerFactory loggerFactory;
    public Slf4jConfiguration.Level loggerLevel;
    public ContainerCapsule containerCapsule;
    public Lookup lookup;
    public SettingsBuilder settingsBuilder;

    public boolean interactive;
    public Path localRepositoryPath;
    public Path installationSettingsPath;
    public Path projectSettingsPath;
    public Path userSettingsPath;
    public Settings effectiveSettings;

    @Override
    public void close() throws InvokerException {
        if (containerCapsule != null) {
            containerCapsule.close();
        }
    }
}
