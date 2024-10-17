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

import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogLevelRecorderTest {
    @Test
    void createsLogLevelRecorder() {
        DefaultLogLevelRecorder logLevelRecorder = new DefaultLogLevelRecorder("WARN");
        logLevelRecorder.record(Level.ERROR);

        assertTrue(logLevelRecorder.metThreshold());
    }

    @Test
    void failsOnLowerThanWarn() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultLogLevelRecorder("INFO"));
    }

    @Test
    void createsLogLevelRecorderWithWarning() {
        DefaultLogLevelRecorder logLevelRecorder = new DefaultLogLevelRecorder("WARNING");
        logLevelRecorder.record(Level.ERROR);

        assertTrue(logLevelRecorder.metThreshold());
    }

    @Test
    void failsOnUnknownLogLevel() {
        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> new DefaultLogLevelRecorder("SEVERE"));
        String message = thrown.getMessage();
        assertThat(message, containsString("SEVERE is not a valid log severity threshold"));
        assertThat(message, containsString("WARN"));
        assertThat(message, containsString("WARNING"));
        assertThat(message, containsString("ERROR"));
    }
}
