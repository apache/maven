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
package org.apache.maven.logging.impl;

import org.apache.maven.api.logging.LogLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultLoggerManagerTest {

    @Test
    void hasReachedTracksMaxLevel() {
        DefaultLoggerManager manager = new DefaultLoggerManager();

        assertFalse(manager.hasReached(LogLevel.WARN));

        manager.record(LogLevel.INFO);
        assertFalse(manager.hasReached(LogLevel.WARN));

        manager.record(LogLevel.WARN);
        assertTrue(manager.hasReached(LogLevel.WARN));

        manager.record(LogLevel.INFO);
        assertTrue(manager.hasReached(LogLevel.WARN), "max should not decrease");
    }

    @Test
    void reconfigureResetsMaxLevel() {
        DefaultLoggerManager manager = new DefaultLoggerManager();
        manager.record(LogLevel.ERROR);
        assertTrue(manager.hasReached(LogLevel.WARN));

        manager.reconfigure();
        assertFalse(manager.hasReached(LogLevel.WARN));
    }

    @Test
    void isLevelEnabledDefaultsToInfo() {
        DefaultLoggerManager manager = new DefaultLoggerManager();

        assertTrue(manager.isLevelEnabled("test", LogLevel.INFO));
        assertTrue(manager.isLevelEnabled("test", LogLevel.WARN));
        assertTrue(manager.isLevelEnabled("test", LogLevel.ERROR));
        assertFalse(manager.isLevelEnabled("test", LogLevel.DEBUG));
        assertFalse(manager.isLevelEnabled("test", LogLevel.TRACE));
    }
}
