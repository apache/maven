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

import org.apache.maven.api.cli.mvn.MavenInvokerRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.cli.mvn.local.LocalMavenInvoker;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.invoker.mvn.DefaultMavenInvoker;

public class DefaultLocalMavenInvoker
        extends DefaultMavenInvoker<
                MavenOptions, MavenInvokerRequest<MavenOptions>, DefaultLocalMavenInvoker.LocalContext>
        implements LocalMavenInvoker {

    public static class LocalContext
            extends DefaultMavenInvoker.MavenContext<
                    MavenOptions, MavenInvokerRequest<MavenOptions>, DefaultLocalMavenInvoker.LocalContext> {
        protected LocalContext(DefaultLocalMavenInvoker invoker, MavenInvokerRequest<MavenOptions> invokerRequest) {
            super(invoker, invokerRequest);
        }
    }

    public DefaultLocalMavenInvoker(ProtoLookup protoLookup) {
        super(protoLookup);
    }

    @Override
    protected LocalContext createContext(MavenInvokerRequest<MavenOptions> invokerRequest) {
        return new LocalContext(this, invokerRequest);
    }
}
