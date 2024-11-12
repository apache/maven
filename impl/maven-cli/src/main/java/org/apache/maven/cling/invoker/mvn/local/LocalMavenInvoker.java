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
package org.apache.maven.cling.invoker.mvn.local;

import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvn.MavenContext;
import org.apache.maven.cling.invoker.mvn.MavenInvoker;

/**
 * Local resident invoker implementation, similar to "local" but keeps Maven instance resident. This implies, that
 * things like environment, system properties, extensions etc. are loaded only once. It is caller duty to ensure
 * that subsequent call is right for the resident instance (ie no env change or different extension needed).
 */
public class LocalMavenInvoker extends MavenInvoker<MavenContext> {
    public LocalMavenInvoker(ProtoLookup protoLookup) {
        super(protoLookup);
    }

    @Override
    protected MavenContext createContext(InvokerRequest invokerRequest) throws InvokerException {
        return new MavenContext(invokerRequest);
    }
}
