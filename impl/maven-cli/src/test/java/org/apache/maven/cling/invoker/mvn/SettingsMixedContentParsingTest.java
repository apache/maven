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
package org.apache.maven.cling.invoker.mvn;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.apache.maven.api.settings.Alias;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.settings.v4.SettingsStaxReader;
import org.apache.maven.settings.v4.SettingsStaxWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the generated StAX reader handles mixed-content fields correctly:
 * both inline text and structured {@code <arg>} children must produce the same
 * parsed model for {@code <options>} and {@code <alias>}.
 */
class SettingsMixedContentParsingTest {

    private static final String NS = "http://maven.apache.org/SETTINGS/2.1.0";

    private Settings parse(String xml) throws Exception {
        return new SettingsStaxReader().read(new StringReader(xml), false, null);
    }

    // ------------------------------------------------------------------ <options>

    @Test
    void optionsInlineText() throws Exception {
        String xml =
                "<settings xmlns=\"" + NS + "\">" + "<options>-T4 --no-transfer-progress</options>" + "</settings>";
        Settings s = parse(xml);
        assertEquals(List.of("-T4", "--no-transfer-progress"), s.getArgs());
    }

    @Test
    void optionsStructuredArgs() throws Exception {
        String xml = "<settings xmlns=\"" + NS + "\">"
                + "<options><arg>-T4</arg><arg>--no-transfer-progress</arg></options>"
                + "</settings>";
        Settings s = parse(xml);
        assertEquals(List.of("-T4", "--no-transfer-progress"), s.getArgs());
    }

    @Test
    void optionsInlineAndStructuredProduceSameResult() throws Exception {
        String inline = "<settings xmlns=\"" + NS + "\">"
                + "<options>-T4 --no-transfer-progress -Djava.version=21</options>"
                + "</settings>";
        String structured = "<settings xmlns=\"" + NS + "\">"
                + "<options>"
                + "<arg>-T4</arg>"
                + "<arg>--no-transfer-progress</arg>"
                + "<arg>-Djava.version=21</arg>"
                + "</options>"
                + "</settings>";
        assertEquals(parse(inline).getArgs(), parse(structured).getArgs());
    }

    @Test
    void optionsInlineQuotedSpaces() throws Exception {
        // Shell-style quoting: -Dmessage="hello world" → one token with a space
        String xml = "<settings xmlns=\"" + NS + "\">" + "<options>-Dmessage=\"hello world\"</options>" + "</settings>";
        Settings s = parse(xml);
        assertEquals(List.of("-Dmessage=hello world"), s.getArgs());
    }

    @Test
    void optionsStructuredSpaceInValue() throws Exception {
        // Structured form: space preserved literally inside the <arg> value
        String xml = "<settings xmlns=\"" + NS + "\">"
                + "<options><arg>-Dmessage=hello world</arg></options>"
                + "</settings>";
        Settings s = parse(xml);
        assertEquals(List.of("-Dmessage=hello world"), s.getArgs());
    }

    @Test
    void optionsEmpty() throws Exception {
        String xml = "<settings xmlns=\"" + NS + "\"><options/></settings>";
        Settings s = parse(xml);
        assertEquals(List.of(), s.getArgs());
    }

    // ------------------------------------------------------------------ <alias>

    @Test
    void aliasInlineText() throws Exception {
        String xml = "<settings xmlns=\"" + NS + "\">"
                + "<alias name=\"ci\">verify -Dmaven.test.skip=true</alias>"
                + "</settings>";
        Settings s = parse(xml);
        assertEquals(1, s.getAliases().size());
        Alias a = s.getAliases().get(0);
        assertEquals("ci", a.getName());
        assertEquals(List.of("verify", "-Dmaven.test.skip=true"), a.getArgs());
    }

    @Test
    void aliasStructuredArgs() throws Exception {
        String xml = "<settings xmlns=\"" + NS + "\">"
                + "<alias name=\"ci\"><arg>verify</arg><arg>-Dmaven.test.skip=true</arg></alias>"
                + "</settings>";
        Settings s = parse(xml);
        assertEquals(1, s.getAliases().size());
        Alias a = s.getAliases().get(0);
        assertEquals("ci", a.getName());
        assertEquals(List.of("verify", "-Dmaven.test.skip=true"), a.getArgs());
    }

    @Test
    void aliasInlineAndStructuredProduceSameResult() throws Exception {
        String inline = "<settings xmlns=\"" + NS + "\">"
                + "<alias name=\"ci\">verify -Dmaven.test.skip=true</alias>"
                + "</settings>";
        String structured = "<settings xmlns=\"" + NS + "\">"
                + "<alias name=\"ci\"><arg>verify</arg><arg>-Dmaven.test.skip=true</arg></alias>"
                + "</settings>";
        assertEquals(
                parse(inline).getAliases().get(0).getArgs(),
                parse(structured).getAliases().get(0).getArgs());
    }

    @Test
    void aliasInlineQuotedSpaces() throws Exception {
        String xml = "<settings xmlns=\"" + NS + "\">"
                + "<alias name=\"msg\">deploy -Dmessage=\"hello world\"</alias>"
                + "</settings>";
        Settings s = parse(xml);
        assertEquals(
                List.of("deploy", "-Dmessage=hello world"),
                s.getAliases().get(0).getArgs());
    }

    @Test
    void aliasStructuredSpaceInValue() throws Exception {
        String xml = "<settings xmlns=\"" + NS + "\">"
                + "<alias name=\"msg\"><arg>deploy</arg><arg>-Dmessage=hello world</arg></alias>"
                + "</settings>";
        Settings s = parse(xml);
        assertEquals(
                List.of("deploy", "-Dmessage=hello world"),
                s.getAliases().get(0).getArgs());
    }

    @Test
    void multipleAliases() throws Exception {
        String xml = "<settings xmlns=\"" + NS + "\">"
                + "<alias name=\"ci\">verify -Dmaven.test.skip=true</alias>"
                + "<alias name=\"release\"><arg>deploy</arg><arg>-Prelease</arg></alias>"
                + "</settings>";
        Settings s = parse(xml);
        assertEquals(2, s.getAliases().size());
        assertEquals(
                List.of("verify", "-Dmaven.test.skip=true"),
                s.getAliases().get(0).getArgs());
        assertEquals(List.of("deploy", "-Prelease"), s.getAliases().get(1).getArgs());
    }

    // ------------------------------------------------------------------ round-trip

    private String write(Settings s) throws Exception {
        StringWriter sw = new StringWriter();
        new SettingsStaxWriter().write(sw, s);
        return sw.toString();
    }

    @Test
    void optionsRoundTrip() throws Exception {
        // Parse structured form → writer produces <options><arg>…</arg></options> → re-parse → same list
        String xml = "<settings xmlns=\"" + NS + "\">"
                + "<options><arg>-T4</arg><arg>--no-transfer-progress</arg></options>"
                + "</settings>";
        Settings original = parse(xml);
        String serialized = write(original);
        Settings roundTripped = parse(serialized);
        assertEquals(
                original.getArgs(),
                roundTripped.getArgs(),
                "options round-trip must preserve args; serialized: " + serialized);
    }

    @Test
    void optionsInlineRoundTrip() throws Exception {
        // Parse inline form → writer produces structured form → re-parse → same list
        String xml =
                "<settings xmlns=\"" + NS + "\">" + "<options>-T4 --no-transfer-progress</options>" + "</settings>";
        Settings original = parse(xml);
        String serialized = write(original);
        // The serialized form must contain the <options> wrapper (not bare <options>value</options>)
        // and re-parsing it must produce the same arg list
        Settings roundTripped = parse(serialized);
        assertEquals(
                original.getArgs(),
                roundTripped.getArgs(),
                "options inline→structured round-trip must preserve args; serialized: " + serialized);
    }

    @Test
    void aliasRoundTrip() throws Exception {
        // Parse structured alias → writer → re-parse
        String xml = "<settings xmlns=\"" + NS + "\">"
                + "<alias name=\"ci\"><arg>verify</arg><arg>-Dmaven.test.skip=true</arg></alias>"
                + "</settings>";
        Settings original = parse(xml);
        String serialized = write(original);
        Settings roundTripped = parse(serialized);
        assertEquals(
                original.getAliases().get(0).getArgs(),
                roundTripped.getAliases().get(0).getArgs(),
                "alias round-trip must preserve args; serialized: " + serialized);
    }

    @Test
    void aliasInlineRoundTrip() throws Exception {
        // Parse inline alias → writer → re-parse
        String xml = "<settings xmlns=\"" + NS + "\">"
                + "<alias name=\"ci\">verify -Dmaven.test.skip=true</alias>"
                + "</settings>";
        Settings original = parse(xml);
        String serialized = write(original);
        Settings roundTripped = parse(serialized);
        assertEquals(
                original.getAliases().get(0).getArgs(),
                roundTripped.getAliases().get(0).getArgs(),
                "alias inline→structured round-trip must preserve args; serialized: " + serialized);
    }
}
