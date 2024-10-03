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
import org.apache.maven.api.cli.mvn.resident.ResidentMavenInvoker;
import org.apache.maven.api.cli.mvn.resident.ResidentMavenInvokerRequest;
import org.apache.maven.api.cli.mvn.resident.ResidentMavenOptions;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvn.DefaultMavenInvoker;

/**
 * Local invoker implementation, when Maven CLI is being run. System uses ClassWorld launcher, and class world
 * instance is passed in via "enhanced" main method. Hence, this class expects fully setup ClassWorld via constructor.
 */
public class DefaultResidentMavenInvoker
        extends DefaultMavenInvoker<
                ResidentMavenOptions, ResidentMavenInvokerRequest, DefaultResidentMavenInvoker.LocalContext>
        implements ResidentMavenInvoker {

    protected static class LocalContext
            extends DefaultMavenInvoker.MavenContext<
                    ResidentMavenOptions, ResidentMavenInvokerRequest, DefaultResidentMavenInvoker.LocalContext> {
        protected LocalContext(DefaultResidentMavenInvoker invoker, ResidentMavenInvokerRequest invokerRequest) {
            super(invoker, invokerRequest);
        }

        @Override
        public void close() throws InvokerException {
            // we are resident, we do not shut down here
        }

        public void shutDown() throws InvokerException {
            super.close();
        }
    }

    private final ConcurrentHashMap<String, LocalContext> contextPool;

    public DefaultResidentMavenInvoker(ProtoLookup protoLookup) {
        super(protoLookup);
        this.contextPool = new ConcurrentHashMap<>();
    }

    @Override
    public void close() throws InvokerException {
        ArrayList<InvokerException> exceptions = new ArrayList<>();
        for (LocalContext context : contextPool.values()) {
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
    protected LocalContext createContext(ResidentMavenInvokerRequest invokerRequest) {
        String id = "?";
        return contextPool.computeIfAbsent(id, k -> new LocalContext(this, invokerRequest));
    }
}
