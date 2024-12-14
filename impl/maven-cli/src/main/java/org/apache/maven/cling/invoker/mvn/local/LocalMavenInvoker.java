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
 * Local Maven invoker implementation, that expects all the Maven to be on classpath. It is "one off" by default,
 * everything is created and everything is disposed at the end of invocation.
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
