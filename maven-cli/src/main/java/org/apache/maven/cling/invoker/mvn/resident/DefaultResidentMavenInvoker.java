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
package org.apache.maven.cling.invoker.mvn.resident;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.mvn.MavenInvokerRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.cli.mvn.resident.ResidentMavenInvoker;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvn.DefaultMavenInvoker;

/**
 * Local invoker implementation, when Maven CLI is being run. System uses ClassWorld launcher, and class world
 * instance is passed in via "enhanced" main method. Hence, this class expects fully setup ClassWorld via constructor.
 */
public class DefaultResidentMavenInvoker
        extends DefaultMavenInvoker<
                MavenOptions, MavenInvokerRequest<MavenOptions>, DefaultResidentMavenInvoker.LocalContext>
        implements ResidentMavenInvoker {

    public static class LocalContext
            extends DefaultMavenInvoker.MavenContext<
                    MavenOptions, MavenInvokerRequest<MavenOptions>, DefaultResidentMavenInvoker.LocalContext> {

        protected LocalContext(DefaultResidentMavenInvoker invoker, MavenInvokerRequest<MavenOptions> invokerRequest) {
            super(invoker, invokerRequest);
        }

        @Override
        public void close() throws InvokerException {
            // we are resident, we do not shut down here
        }

        public void shutDown() throws InvokerException {
            super.close();
        }

        public LocalContext copy(MavenInvokerRequest<MavenOptions> invokerRequest) {
            LocalContext shadow = new LocalContext((DefaultResidentMavenInvoker) invoker, invokerRequest);

            shadow.logger = logger;
            shadow.loggerFactory = loggerFactory;
            shadow.loggerLevel = loggerLevel;
            shadow.containerCapsule = containerCapsule;
            shadow.lookup = lookup;
            shadow.settingsBuilder = settingsBuilder;

            shadow.interactive = interactive;
            shadow.localRepositoryPath = localRepositoryPath;
            shadow.installationSettingsPath = installationSettingsPath;
            shadow.projectSettingsPath = projectSettingsPath;
            shadow.userSettingsPath = userSettingsPath;
            shadow.effectiveSettings = effectiveSettings;

            shadow.mavenExecutionRequest = mavenExecutionRequest;
            shadow.eventSpyDispatcher = eventSpyDispatcher;
            shadow.mavenExecutionRequestPopulator = mavenExecutionRequestPopulator;
            shadow.toolchainsBuilder = toolchainsBuilder;
            shadow.modelProcessor = modelProcessor;
            shadow.maven = maven;

            return shadow;
        }
    }

    private final ConcurrentHashMap<String, LocalContext> residentContext;

    public DefaultResidentMavenInvoker(ProtoLookup protoLookup) {
        super(protoLookup);
        this.residentContext = new ConcurrentHashMap<>();
    }

    @Override
    public void close() throws InvokerException {
        ArrayList<InvokerException> exceptions = new ArrayList<>();
        for (LocalContext context : residentContext.values()) {
            try {
                context.shutDown();
            } catch (InvokerException e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            InvokerException exception = new InvokerException("Could not cleanly shut down context pool");
            exceptions.forEach(exception::addSuppressed);
            throw exception;
        }
    }

    @Override
    protected LocalContext createContext(MavenInvokerRequest<MavenOptions> invokerRequest) {
        return residentContext
                .computeIfAbsent(getContextId(invokerRequest), k -> {
                    LocalContext master = new LocalContext(this, invokerRequest);
                    try {
                        validate(master);
                        prepare(master);
                        logging(master);
                        container(master);
                        lookup(master);
                        return master;
                    } catch (Exception e) {
                        throw new InvokerException("Failed to init master context", e);
                    }
                })
                .copy(invokerRequest);
    }

    protected String getContextId(MavenInvokerRequest<MavenOptions> invokerRequest) {
        // TODO: in a moment Maven stop pushing user properties to system properties (and maybe something more)
        // and allow multiple instances per JVM, this may become a pool?
        return "resident";
    }

    @Override
    protected void container(LocalContext context) throws Exception {
        if (context.maven == null) {
            super.container(context);
        }
    }

    @Override
    protected void lookup(LocalContext context) throws Exception {
        if (context.maven == null) {
            super.lookup(context);
        }
    }
}
