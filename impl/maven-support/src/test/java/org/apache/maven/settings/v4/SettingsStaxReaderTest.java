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
package org.apache.maven.settings.v4;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.apache.maven.api.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsStaxReaderTest {

    @Test
    void optionsPreserveQuotedEmptyArgument() throws Exception {
        Settings settings = parse("<options>--log-file \"\" verify</options>");
        assertEquals(List.of("--log-file", "", "verify"), settings.getArgs());
    }

    @Test
    void aliasPreservesQuotedEmptyArgument() throws Exception {
        Settings settings = parse("<alias name=\"ci\">--log-file '' verify</alias>");
        assertEquals(
                List.of("--log-file", "", "verify"),
                settings.getAliases().get(0).getArgs());
    }

    @Test
    void inlineAndStructuredEmptyArgumentsAreEquivalent() throws Exception {
        Settings inline =
                parse("<options>--log-file \"\" verify</options>" + "<alias name=\"ci\">--log-file '' verify</alias>");
        Settings structured = parse("<options><arg>--log-file</arg><arg/><arg>verify</arg></options>"
                + "<alias name=\"ci\"><arg>--log-file</arg><arg/><arg>verify</arg></alias>");

        assertEquals(structured.getArgs(), inline.getArgs());
        assertEquals(
                structured.getAliases().get(0).getArgs(),
                inline.getAliases().get(0).getArgs());
    }

    @Test
    void emptyArgumentsSurviveRoundTrip() throws Exception {
        Settings original =
                parse("<options>--log-file \"\" verify</options>" + "<alias name=\"ci\">--log-file '' verify</alias>");
        StringWriter output = new StringWriter();
        new SettingsStaxWriter().write(output, original);
        Settings roundTripped = new SettingsStaxReader().read(new StringReader(output.toString()), false, null);

        assertEquals(List.of("--log-file", "", "verify"), roundTripped.getArgs());
        assertEquals(
                List.of("--log-file", "", "verify"),
                roundTripped.getAliases().get(0).getArgs());
    }

    private Settings parse(String content) throws Exception {
        String xml = "<settings xmlns=\"http://maven.apache.org/SETTINGS/2.1.0\">" + content + "</settings>";
        return new SettingsStaxReader().read(new StringReader(xml), false, null);
    }
}
