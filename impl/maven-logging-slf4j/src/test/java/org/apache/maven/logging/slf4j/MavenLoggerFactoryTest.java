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
package org.apache.maven.logging.slf4j;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class MavenLoggerFactoryTest {
    @Test
    void createsSimpleLogger() {
        MavenLoggerFactory mavenLoggerFactory = new MavenLoggerFactory();

        Logger logger = mavenLoggerFactory.getLogger("Test");

        assertInstanceOf(MavenSimpleLogger.class, logger, "Expected logger to be instance of MavenSimpleLogger");
    }

    @Test
    void loggerCachingWorks() {
        MavenLoggerFactory mavenLoggerFactory = new MavenLoggerFactory();

        Logger logger = mavenLoggerFactory.getLogger("Test");
        Logger logger2 = mavenLoggerFactory.getLogger("Test");
        Logger differentLogger = mavenLoggerFactory.getLogger("TestWithDifferentName");

        assertNotNull(logger);
        assertNotNull(differentLogger);
        assertSame(logger, logger2);
        assertNotSame(logger, differentLogger);
    }
}
