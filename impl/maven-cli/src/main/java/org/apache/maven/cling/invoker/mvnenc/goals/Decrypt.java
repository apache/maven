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
package org.apache.maven.cling.invoker.mvnenc.goals;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.cling.invoker.mvnenc.EncryptContext;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;

import static org.apache.maven.cling.invoker.mvnenc.EncryptInvoker.BAD_OPERATION;
import static org.apache.maven.cling.invoker.mvnenc.EncryptInvoker.OK;

/**
 * The "decrypt" goal.
 */
@Singleton
@Named("decrypt")
public class Decrypt extends ConfiguredGoalSupport {
    @Inject
    public Decrypt(MessageBuilderFactory messageBuilderFactory, SecDispatcher secDispatcher) {
        super(messageBuilderFactory, secDispatcher);
    }

    @Override
    protected int doExecute(EncryptContext context) throws Exception {
        String encrypted = context.reader.readLine("Enter the password to decrypt: ");
        if (secDispatcher.isAnyEncryptedString(encrypted)) {
            context.terminal.writer().println(secDispatcher.decrypt(encrypted));
            return OK;
        } else {
            context.terminal.writer().println(messageBuilderFactory.builder().error("Malformed encrypted string"));
            return BAD_OPERATION;
        }
    }
}
