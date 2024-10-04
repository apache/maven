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
package org.apache.maven.cling;

import java.io.IOException;

import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.jline.MessageUtils;
import org.codehaus.plexus.classworlds.ClassWorld;

import static java.util.Objects.requireNonNull;

/**
 * The CLI "new-gen".
 *
 * @param <O> the options type
 * @param <R> the request type
 */
public abstract class ClingSupport<O extends Options, R extends InvokerRequest<O>> {
    static final String CORE_CLASS_REALM_ID = "plexus.core";

    protected final ClassWorld classWorld;
    protected final boolean classWorldManaged;

    /**
     * Ctor that creates "managed" ClassWorld. This constructor is not used in "normal" circumstances.
     */
    public ClingSupport() {
        this(new ClassWorld(CORE_CLASS_REALM_ID, Thread.currentThread().getContextClassLoader()), true);
    }

    /**
     * Ctor to be used when running in ClassWorlds Launcher.
     */
    public ClingSupport(ClassWorld classWorld) {
        this(classWorld, false);
    }

    private ClingSupport(ClassWorld classWorld, boolean classWorldManaged) {
        this.classWorld = requireNonNull(classWorld);
        this.classWorldManaged = classWorldManaged;
    }

    /**
     * The main entry point.
     */
    public int run(String[] args) throws IOException {
        MessageUtils.systemInstall();
        MessageUtils.registerShutdownHook();
        try (Invoker<R> invoker = createInvoker()) {
            return invoker.invoke(parseArguments(args));
        } catch (ParserException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (InvokerException e) {
            return 1;
        } finally {
            try {
                if (classWorldManaged) {
                    classWorld.close();
                }
            } finally {
                MessageUtils.systemUninstall();
            }
        }
    }

    protected abstract Invoker<R> createInvoker();

    protected abstract R parseArguments(String[] args) throws ParserException, IOException;
}
