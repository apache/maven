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
package org.apache.maven.cling.invoker.mvnenc;

import org.apache.maven.api.cli.mvnenc.EncryptInvoker;
import org.apache.maven.api.cli.mvnenc.EncryptInvokerRequest;
import org.apache.maven.api.cli.mvnenc.EncryptOptions;
import org.apache.maven.cling.invoker.LookupInvoker;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;

/**
 * Encrypt invoker implementation, when Encrypt CLI is being run. System uses ClassWorld launcher, and class world
 * instance is passed in via "enhanced" main method. Hence, this class expects fully setup ClassWorld via constructor.
 */
public class DefaultEncryptInvoker
        extends LookupInvoker<EncryptOptions, EncryptInvokerRequest, DefaultEncryptInvoker.LocalContext>
        implements EncryptInvoker {

    public static class LocalContext
            extends LookupInvokerContext<EncryptOptions, EncryptInvokerRequest, DefaultEncryptInvoker.LocalContext> {
        protected LocalContext(DefaultEncryptInvoker invoker, EncryptInvokerRequest invokerRequest) {
            super(invoker, invokerRequest);
        }

        protected SecDispatcher secDispatcher;
    }

    public DefaultEncryptInvoker(ProtoLookup protoLookup) {
        super(protoLookup);
    }

    @Override
    protected int execute(LocalContext context) throws Exception {
        return doExecute(context);
    }

    @Override
    protected LocalContext createContext(EncryptInvokerRequest invokerRequest) {
        return new LocalContext(this, invokerRequest);
    }

    @Override
    protected void postContainerCreated(LocalContext context) {
        context.secDispatcher = context.lookup.lookup(SecDispatcher.class);
    }

    protected int doExecute(LocalContext localContext) throws Exception {
        localContext.logger.info("Hello, this is SecDispatcher.");
        localContext.logger.info("Available Ciphers: " + localContext.secDispatcher.availableCiphers());
        localContext.logger.info("Available Dispatchers: " + localContext.secDispatcher.availableDispatchers());
        return 0;
    }
}
