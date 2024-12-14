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
package org.apache.maven.cling.invoker.mvnsh;

import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.cling.invoker.mvnenc.EncryptContext;
import org.apache.maven.cling.invoker.mvnenc.EncryptInvoker;

/**
 * Shell Encrypt invoker: passes over relevant context bits.
 */
public class ShellEncryptInvoker extends EncryptInvoker {
    private final ShellContext shellContext;

    public ShellEncryptInvoker(ShellContext shellContext) {
        super(shellContext.invokerRequest.lookup());
        this.shellContext = shellContext;
    }

    @Override
    protected EncryptContext createContext(InvokerRequest invokerRequest) throws InvokerException {
        EncryptContext result = new EncryptContext(invokerRequest, false);

        result.logger = shellContext.logger;
        result.loggerFactory = shellContext.loggerFactory;
        result.slf4jConfiguration = shellContext.slf4jConfiguration;
        result.loggerLevel = shellContext.loggerLevel;
        result.coloredOutput = shellContext.coloredOutput;
        result.terminal = shellContext.terminal;
        result.writer = shellContext.writer;

        result.containerCapsule = shellContext.containerCapsule;
        result.lookup = shellContext.lookup;
        result.eventSpyDispatcher = shellContext.eventSpyDispatcher;
        return result;
    }
}
