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
package org.apache.maven.logging;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.build.report.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.spi.LocationAwareLogger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProjectBuildLogAppenderTest {

    private final BuildEventListener listener = mock(BuildEventListener.class);

    @AfterEach
    void cleanup() {
        ProjectBuildLogAppender.setReportCapture(null);
    }

    @Test
    void summaryModeSuppressesInlineWarningsButCapturesThemForReport() throws Exception {
        List<LogEvent> captured = new ArrayList<>();
        ProjectBuildLogAppender.setReportCapture(captured::add);

        try (ProjectBuildLogAppender appender = new ProjectBuildLogAppender(listener, "summary")) {

            appender.accept(LocationAwareLogger.WARN_INT, "test.logger", "warning message", "formatted warning", null);

            verify(listener, never()).projectLogMessage(any(), any());
            assertEquals(1, captured.size());
        }
    }

    @Test
    void allModeForwardsWarningsAndCapturesThem() throws Exception {
        List<LogEvent> captured = new ArrayList<>();
        ProjectBuildLogAppender.setReportCapture(captured::add);

        try (ProjectBuildLogAppender appender = new ProjectBuildLogAppender(listener, "all")) {

            appender.accept(LocationAwareLogger.WARN_INT, "test.logger", "warning message", "formatted warning", null);
            verify(listener).projectLogMessage(any(), any());
            assertEquals(1, captured.size());
        }
    }

    @Test
    void summaryModeStillForwardsInfoMessages() throws Exception {
        try (ProjectBuildLogAppender appender = new ProjectBuildLogAppender(listener, "summary")) {

            appender.accept(LocationAwareLogger.INFO_INT, "test.logger", "info message", "formatted info", null);

            verify(listener).projectLogMessage(any(), any());
        }
    }

    @Test
    void summaryModeMatchingIsCaseInsensitive() throws Exception {
        try (ProjectBuildLogAppender appender = new ProjectBuildLogAppender(listener, "SUMMARY")) {
            appender.accept(LocationAwareLogger.WARN_INT, "test.logger", "warning message", "formatted warning", null);

            verify(listener, never()).projectLogMessage(any(), any());
        }
    }
}
