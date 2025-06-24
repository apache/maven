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
package org.apache.maven.slf4j;

import org.apache.maven.logging.api.LogLevelRecorder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenLoggerFactoryTest {
    @Test
    void createsSimpleLogger() {
        MavenLoggerFactory mavenLoggerFactory = new MavenLoggerFactory();

        Logger logger = mavenLoggerFactory.getLogger("Test");

        assertThat(logger, instanceOf(MavenSimpleLogger.class));
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

    @Test
    void reportsWhenFailOnSeverityThresholdHasBeenHit() {
        MavenLoggerFactory mavenLoggerFactory = new MavenLoggerFactory();
        mavenLoggerFactory.logLevelRecorder.setMaxLevelAllowed(LogLevelRecorder.Level.ERROR);

        MavenFailOnSeverityLogger logger = (MavenFailOnSeverityLogger) mavenLoggerFactory.getLogger("Test");
        assertFalse(mavenLoggerFactory.logLevelRecorder.metThreshold());

        logger.warn("This should not hit the fail threshold");
        assertFalse(mavenLoggerFactory.logLevelRecorder.metThreshold());

        logger.error("This should hit the fail threshold");
        assertTrue(mavenLoggerFactory.logLevelRecorder.metThreshold());

        logger.warn("This should not reset the fail threshold");
        assertTrue(mavenLoggerFactory.logLevelRecorder.metThreshold());
    }
}
