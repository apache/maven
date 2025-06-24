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

import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.cling.invoker.mvnenc.EncryptContext;
import org.apache.maven.cling.invoker.mvnenc.EncryptInvoker;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;

/**
 * The support class for interactive goal implementations.
 */
public abstract class InteractiveGoalSupport extends GoalSupport {
    protected InteractiveGoalSupport(MessageBuilderFactory messageBuilderFactory, SecDispatcher secDispatcher) {
        super(messageBuilderFactory, secDispatcher);
    }

    @Override
    public int execute(EncryptContext context) throws Exception {
        if (!context.interactive) {
            context.terminal.writer().println("This tool works only in interactive mode!");
            context.terminal
                    .writer()
                    .println("Tool purpose is to configure password management on developer workstations.");
            context.terminal
                    .writer()
                    .println(
                            "Note: Generated configuration can be moved/copied to headless environments, if configured as such.");
            return EncryptInvoker.BAD_OPERATION;
        }

        return doExecute(context);
    }

    protected abstract int doExecute(EncryptContext context) throws Exception;
}
