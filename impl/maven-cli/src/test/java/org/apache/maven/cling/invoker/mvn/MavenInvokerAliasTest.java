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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.settings.Alias;
import org.apache.maven.api.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@link MavenInvoker#addAliases}: verifies that alias inline text
 * and structured {@code <arg>} forms both expand correctly.
 *
 * <p>Covered syntax:
 * <pre>{@code
 * <!-- inline text -->
 * <alias name="ci">verify -Dmaven.test.skip=true</alias>
 *
 * <!-- structured args -->
 * <alias name="ci">
 *   <arg>verify</arg>
 *   <arg>-Dmaven.test.skip=true</arg>
 * </alias>
 * }</pre>
 */
class MavenInvokerAliasTest {

    // ------------------------------------------------------------------ inline text form

    @Test
    void inlineAliasSingleToken() {
        // <alias name="ci">verify</alias>
        Alias alias = Alias.newBuilder().name("ci").content("verify").build();
        Settings settings = Settings.newBuilder().aliases(List.of(alias)).build();

        Map<String, List<String>> result = new HashMap<>();
        MavenInvoker.addAliases(result, settings);

        assertEquals(List.of("verify"), result.get("ci"));
    }

    @Test
    void inlineAliasMultipleTokens() {
        // <alias name="ci">verify -Dmaven.test.skip=true</alias>
        Alias alias = Alias.newBuilder()
                .name("ci")
                .content("    verify -Dmaven.test.skip=true\n")
                .build();
        Settings settings = Settings.newBuilder().aliases(List.of(alias)).build();

        Map<String, List<String>> result = new HashMap<>();
        MavenInvoker.addAliases(result, settings);

        assertEquals(List.of("verify", "-Dmaven.test.skip=true"), result.get("ci"));
    }

    @Test
    void inlineAliasQuotedSpaces() {
        // <alias name="msg">-Dmessage="hello world"</alias>
        Alias alias = Alias.newBuilder()
                .name("msg")
                .content("-Dmessage=\"hello world\"")
                .build();
        Settings settings = Settings.newBuilder().aliases(List.of(alias)).build();

        Map<String, List<String>> result = new HashMap<>();
        MavenInvoker.addAliases(result, settings);

        assertEquals(List.of("-Dmessage=hello world"), result.get("msg"));
    }

    // ------------------------------------------------------------------ structured <arg> form

    @Test
    void structuredAliasSingleArg() {
        // <alias name="ci"><arg>verify</arg></alias>
        Alias alias = Alias.newBuilder().name("ci").args(List.of("verify")).build();
        Settings settings = Settings.newBuilder().aliases(List.of(alias)).build();

        Map<String, List<String>> result = new HashMap<>();
        MavenInvoker.addAliases(result, settings);

        assertEquals(List.of("verify"), result.get("ci"));
    }

    @Test
    void structuredAliasMultipleArgs() {
        // <alias name="ci">
        //   <arg>verify</arg>
        //   <arg>-Dmaven.test.skip=true</arg>
        // </alias>
        Alias alias = Alias.newBuilder()
                .name("ci")
                .args(List.of("verify", "-Dmaven.test.skip=true"))
                .build();
        Settings settings = Settings.newBuilder().aliases(List.of(alias)).build();

        Map<String, List<String>> result = new HashMap<>();
        MavenInvoker.addAliases(result, settings);

        assertEquals(List.of("verify", "-Dmaven.test.skip=true"), result.get("ci"));
    }

    @Test
    void structuredAliasArgValueWithSpaces() {
        // <alias name="msg"><arg>-Dmessage=hello world</arg></alias>
        // The structured form preserves spaces literally — no tokenization needed.
        Alias alias = Alias.newBuilder()
                .name("msg")
                .args(List.of("-Dmessage=hello world"))
                .build();
        Settings settings = Settings.newBuilder().aliases(List.of(alias)).build();

        Map<String, List<String>> result = new HashMap<>();
        MavenInvoker.addAliases(result, settings);

        assertEquals(List.of("-Dmessage=hello world"), result.get("msg"));
    }

    // ------------------------------------------------------------------ last-wins / removal

    @Test
    void emptyAliasRemovesInherited() {
        // An alias with no content and no args removes an inherited alias (last-wins across tiers).
        Alias alias = Alias.newBuilder().name("ci").build();
        Settings settings = Settings.newBuilder().aliases(List.of(alias)).build();

        Map<String, List<String>> result = new HashMap<>();
        result.put("ci", List.of("install")); // simulates inherited alias from a lower-priority tier
        MavenInvoker.addAliases(result, settings);

        assertNull(result.get("ci"), "empty alias should remove the inherited entry");
    }

    @Test
    void laterTierAliasOverridesEarlier() {
        // Project settings override user settings for the same alias name.
        Alias userAlias = Alias.newBuilder().name("ci").content("install").build();
        Alias projectAlias = Alias.newBuilder()
                .name("ci")
                .args(List.of("verify", "-Dmaven.test.skip=true"))
                .build();

        Settings userSettings =
                Settings.newBuilder().aliases(List.of(userAlias)).build();
        Settings projectSettings =
                Settings.newBuilder().aliases(List.of(projectAlias)).build();

        Map<String, List<String>> result = new HashMap<>();
        MavenInvoker.addAliases(result, userSettings);
        MavenInvoker.addAliases(result, projectSettings);

        assertEquals(List.of("verify", "-Dmaven.test.skip=true"), result.get("ci"));
    }

    @Test
    void nullNameAliasIgnored() {
        Alias alias = Alias.newBuilder().content("verify").build(); // name is null
        Settings settings = Settings.newBuilder().aliases(List.of(alias)).build();

        Map<String, List<String>> result = new HashMap<>();
        MavenInvoker.addAliases(result, settings);

        assertEquals(Map.of(), result);
    }
}
