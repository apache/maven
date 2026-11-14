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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AnsiTerminalTest {

    @Test
    void parsesWidth() {
        assertEquals(137, AnsiTerminal.parseWidth("137"));
        assertEquals(137, AnsiTerminal.parseWidth(" 137\n"));
        assertEquals(-1, AnsiTerminal.parseWidth("0"));
        assertEquals(-1, AnsiTerminal.parseWidth("wide"));
        assertEquals(-1, AnsiTerminal.parseWidth(null));
    }

    @Test
    void noTerminalUntilDetected() {
        // tests never call detect(), like embedded Maven: "auto" color must stay off
        assertFalse(AnsiTerminal.isTty());
    }
}
