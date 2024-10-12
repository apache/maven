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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.cling.invoker.mvnenc.DefaultEncryptInvoker;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;

import static org.apache.maven.cling.invoker.mvnenc.DefaultEncryptInvoker.ERROR;

/**
 * The support class for goal implementations that requires valid/workable config.
 */
public abstract class ConfiguredGoalSupport extends GoalSupport {
    protected ConfiguredGoalSupport(MessageBuilderFactory messageBuilderFactory, SecDispatcher secDispatcher) {
        super(messageBuilderFactory, secDispatcher);
    }

    @Override
    public int execute(DefaultEncryptInvoker.LocalContext context) throws Exception {
        if (!validateConfiguration()) {
            logger.error(messageBuilderFactory
                    .builder()
                    .error("Maven Encryption is not configured, run `mvnenc init` first.")
                    .build());
            return ERROR;
        }
        return doExecute(context);
    }

    protected boolean validateConfiguration() {
        SecDispatcher.ValidationResponse response = secDispatcher.validateConfiguration();
        if (!response.isValid() || logger.isDebugEnabled()) {
            dumpResponse("", response);
        }
        return response.isValid();
    }

    protected void dumpResponse(String indent, SecDispatcher.ValidationResponse response) {
        logger.info(
                response.isValid()
                        ? messageBuilderFactory
                                .builder()
                                .success("{}Configuration validation of {}: {}")
                                .build()
                        : messageBuilderFactory
                                .builder()
                                .failure("{}Configuration validation of {}: {}")
                                .build(),
                indent,
                response.getSource(),
                response.isValid() ? "VALID" : "INVALID");
        for (Map.Entry<SecDispatcher.ValidationResponse.Level, List<String>> entry :
                response.getReport().entrySet()) {
            Consumer<String> consumer =
                    s -> logger.info(messageBuilderFactory.builder().info(s).build());
            if (entry.getKey() == SecDispatcher.ValidationResponse.Level.ERROR) {
                consumer = s ->
                        logger.error(messageBuilderFactory.builder().error(s).build());
            } else if (entry.getKey() == SecDispatcher.ValidationResponse.Level.WARNING) {
                consumer = s ->
                        logger.warn(messageBuilderFactory.builder().warning(s).build());
            }
            for (String line : entry.getValue()) {
                consumer.accept(indent + "  " + line);
            }
        }
        for (SecDispatcher.ValidationResponse subsystem : response.getSubsystems()) {
            dumpResponse(indent + "  ", subsystem);
        }
    }

    protected abstract int doExecute(DefaultEncryptInvoker.LocalContext context) throws Exception;
}
