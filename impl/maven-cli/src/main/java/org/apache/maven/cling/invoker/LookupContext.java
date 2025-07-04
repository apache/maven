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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Logger;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.toolchain.PersistedToolchains;
import org.apache.maven.cling.logging.Slf4jConfiguration;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.logging.BuildEventListener;
import org.jline.terminal.Terminal;
import org.slf4j.ILoggerFactory;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("VisibilityModifier")
public class LookupContext implements AutoCloseable {
    public final InvokerRequest invokerRequest;
    public final CWD cwd;
    public final Path installationDirectory;
    public final Path userDirectory;
    public final boolean containerCapsuleManaged;
    private final Options options;

    public LookupContext(InvokerRequest invokerRequest, boolean containerCapsuleManaged, Options options) {
        this.invokerRequest = requireNonNull(invokerRequest);
        this.cwd = CWD.create(invokerRequest.cwd());
        this.installationDirectory = CliUtils.getCanonicalPath(invokerRequest.installationDirectory());
        this.userDirectory = CliUtils.getCanonicalPath(invokerRequest.userHomeDirectory());
        this.containerCapsuleManaged = containerCapsuleManaged;
        this.options = options;
        this.logger = invokerRequest.parserRequest().logger();

        Map<String, String> user = new HashMap<>(invokerRequest.userProperties());
        user.put("session.topDirectory", invokerRequest.topDirectory().toString());
        if (invokerRequest.rootDirectory().isPresent()) {
            user.put(
                    "session.rootDirectory",
                    invokerRequest.rootDirectory().get().toString());
        }
        this.protoSession = ProtoSession.newBuilder()
                .withSystemProperties(invokerRequest.systemProperties())
                .withUserProperties(user)
                .withTopDirectory(invokerRequest.topDirectory())
                .withRootDirectory(invokerRequest.rootDirectory().orElse(null))
                .build();
    }

    public Logger logger;

    // this one "evolves" as process progresses (instance is immutable but instances are replaced)
    public ProtoSession protoSession;
    // here we track which user properties we pushed to Java System Properties (internal only)
    public Set<String> pushedUserProperties;

    public ILoggerFactory loggerFactory;
    public Slf4jConfiguration slf4jConfiguration;
    public Slf4jConfiguration.Level loggerLevel;
    public Boolean coloredOutput;
    public Terminal terminal;
    public Consumer<String> writer;

    public ContainerCapsule containerCapsule;
    public Lookup lookup;
    public EventSpyDispatcher eventSpyDispatcher;

    public BuildEventListener buildEventListener;

    // paths user can override from CLI, and we need to set on MavenExReq
    public Path installationSettingsPath;
    public Path projectSettingsPath;
    public Path userSettingsPath;
    public boolean interactive;
    public Path localRepositoryPath;
    public Settings effectiveSettings;
    public PersistedToolchains effectiveToolchains;

    public final List<AutoCloseable> closeables = new ArrayList<>();

    @Override
    public void close() throws InvokerException {
        List<Exception> causes = null;
        List<AutoCloseable> cs = new ArrayList<>(closeables);
        Collections.reverse(cs);
        for (AutoCloseable c : cs) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    if (causes == null) {
                        causes = new ArrayList<>();
                    }
                    causes.add(e);
                }
            }
        }
        if (causes != null) {
            InvokerException exception = new InvokerException("Unable to close context");
            causes.forEach(exception::addSuppressed);
            throw exception;
        }
    }

    public final void closeContainer() throws Exception {
        if (containerCapsuleManaged) {
            doCloseContainer();
        }
    }

    public void doCloseContainer() throws Exception {
        if (containerCapsule != null) {
            try {
                containerCapsule.close();
            } finally {
                eventSpyDispatcher = null;
                lookup = null;
                containerCapsule = null;
            }
        }
    }

    public Options options() {
        return options;
    }
}
