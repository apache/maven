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
    }

    public DefaultResidentMavenInvoker(ProtoLookup protoLookup) {
        super(protoLookup);
    }

    @Override
    public void close() throws InvokerException {
        // TODO: shutdown
    }

    @Override
    protected LocalContext createContext(ResidentMavenInvokerRequest invokerRequest) {
        return new LocalContext(this, invokerRequest);
    }
}
