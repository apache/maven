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
package org.apache.maven.cli.logging.impl;

import org.apache.maven.cli.logging.BaseSlf4jConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for slf4j-logback.
 *
 * @author Hervé Boutemy
 * @since 3.1.0
 */
public class LogbackConfiguration extends BaseSlf4jConfiguration {
    @Override
    public void setRootLoggerLevel(Level level) {
        ch.qos.logback.classic.Level value;
        switch (level) {
            case DEBUG:
                value = ch.qos.logback.classic.Level.DEBUG;
                break;

            case INFO:
                value = ch.qos.logback.classic.Level.INFO;
                break;

            default:
                value = ch.qos.logback.classic.Level.ERROR;
                break;
        }
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(value);
    }

    @Override
    public void activate() {
        // no op
    }
}
