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

/**
 * Configuration available for users setting a custom binding and not using --fail-on-severity to avoid warnings.
 * This is basically a no-op implementation not issuing any warning.
 * To use it add to maven classpath (m2.conf) a folder/jar with a META-INF/maven/slf4j-configuration.properties
 * containing {@code loggerClass = org.apache.maven.cli.logging.impl.NoopConfiguration}.
 */
public class NoopConfiguration extends BaseSlf4jConfiguration {
    @Override
    public void setRootLoggerLevel(Level level) {
        // no-op
    }

    @Override
    public void activate() {
        // no-op
    }
}
