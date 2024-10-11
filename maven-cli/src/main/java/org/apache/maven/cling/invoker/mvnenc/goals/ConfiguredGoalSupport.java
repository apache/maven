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

import org.apache.maven.cling.invoker.mvnenc.DefaultEncryptInvoker;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;

import static org.apache.maven.cling.invoker.mvnenc.DefaultEncryptInvoker.ERROR;

/**
 * The support class for goal implementations.
 */
public abstract class ConfiguredGoalSupport extends GoalSupport {
    protected ConfiguredGoalSupport(SecDispatcher secDispatcher) {
        super(secDispatcher);
    }

    @Override
    public int execute(DefaultEncryptInvoker.LocalContext context) throws Exception {
        if (!configExists()) {
            context.logger.error("Encryption is not configured, run `mvnenc init` first.");
            return ERROR;
        }
        return doExecute(context);
    }

    protected abstract int doExecute(DefaultEncryptInvoker.LocalContext context) throws Exception;
}
