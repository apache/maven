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

import java.io.IOException;

import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.cling.invoker.mvnenc.Goal;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The support class for goal implementations.
 */
public abstract class GoalSupport implements Goal {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final MessageBuilderFactory messageBuilderFactory;
    protected final SecDispatcher secDispatcher;

    protected GoalSupport(MessageBuilderFactory messageBuilderFactory, SecDispatcher secDispatcher) {
        this.messageBuilderFactory = messageBuilderFactory;
        this.secDispatcher = secDispatcher;
    }

    protected boolean configExists() throws IOException {
        return secDispatcher.readConfiguration(false) != null;
    }
}
