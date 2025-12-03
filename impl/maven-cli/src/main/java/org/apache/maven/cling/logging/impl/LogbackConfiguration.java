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
package org.apache.maven.cling.logging.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.maven.cling.logging.BaseSlf4jConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for slf4j-logback.
 *
 * @since 3.1.0
 */
public class LogbackConfiguration extends BaseSlf4jConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogbackConfiguration.class);

    @Override
    public void setRootLoggerLevel(Level level) {
        try {
            Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
            Field levelField =
                    switch (level) {
                        case DEBUG -> levelClass.getField("DEBUG");
                        case INFO -> levelClass.getField("INFO");
                        default -> levelClass.getField("ERROR");
                    };

            Method setLevelMethod = levelClass.getMethod("setLevel", levelClass);
            setLevelMethod.invoke(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME), levelField.get(null));
        } catch (ClassNotFoundException
                | NoSuchFieldException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            LOGGER.error("Failed to configure logback", e);
        }
    }

    @Override
    public void activate() {
        // no op
    }
}
