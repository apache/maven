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
package org.apache.maven.cli.logging;

/**
 * Interface for configuration operations on loggers, which are not available in slf4j, then require per-slf4f-binding
 * implementation.
 *
 * @author Hervé Boutemy
 * @since 3.1.0
 */
public interface Slf4jConfiguration {
    /**
     * Level
     */
    enum Level {
        DEBUG,
        INFO,
        ERROR
    }

    /**
     * Set root logging level.
     *
     * @param level the level
     */
    void setRootLoggerLevel(Level level);

    /**
     * Activate logging implementation configuration (if necessary).
     */
    void activate();
}
