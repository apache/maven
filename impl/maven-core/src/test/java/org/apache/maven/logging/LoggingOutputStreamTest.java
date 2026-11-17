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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LoggingOutputStreamTest {

    @Test
    void forceFlushAfterCloseDoesNotThrow() {
        List<String> lines = new ArrayList<>();
        LoggingOutputStream los = new LoggingOutputStream(lines::add);
        PrintStream ps = los.printStream();
        ps.print("unterminated");
        ps.close();

        assertDoesNotThrow(((LoggingOutputStream.LoggingPrintStream) ps)::forceFlush);
        assertEquals(List.of(), lines);
    }

    @Test
    void forceFlushBeforeCloseFlushesPendingLine() {
        List<String> lines = new ArrayList<>();
        LoggingOutputStream los = new LoggingOutputStream(lines::add);
        PrintStream ps = los.printStream();
        ps.print("unterminated");

        ((LoggingOutputStream.LoggingPrintStream) ps).forceFlush();

        assertEquals(List.of("unterminated"), lines);
    }
}
