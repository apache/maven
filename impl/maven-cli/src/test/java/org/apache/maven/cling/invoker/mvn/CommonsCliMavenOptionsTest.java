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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonsCliMavenOptionsTest {
    private final ParserRequest request =
            ParserRequest.mvn(new String[0], new JLineMessageBuilderFactory()).build();

    static Stream<List<String>> updateOptions() {
        return Stream.of(
                List.of("-U"),
                List.of("--update-all"),
                List.of("--update-snapshots"),
                List.of("-U", "--update-all", "--update-snapshots"),
                List.of("--update-snapshots", "--update-all", "-U"));
    }

    @ParameterizedTest
    @MethodSource("updateOptions")
    void updateOptionsForceUpdates(List<String> args) throws ParseException {
        CommonsCliMavenOptions options = CommonsCliMavenOptions.parse("test", args.toArray(new String[0]));
        assertTrue(options.updateAll().orElseThrow());
        assertTrue(options.artifactsUpdatePolicy().isEmpty());
        assertTrue(options.metadataUpdatePolicy().isEmpty());

        List<String> warnings = new ArrayList<>();
        options.warnAboutDeprecatedOptions(request, warnings::add);
        if (args.contains("--update-snapshots")) {
            assertEquals(2, warnings.size());
            assertEquals("Detected deprecated option use in test", warnings.get(0));
            assertTrue(warnings.get(1).contains("The option --update-snapshots is deprecated"));
            assertTrue(warnings.get(1).contains("4.2.0: Use -U,--update-all instead."));
            assertFalse(warnings.get(1).contains("-U,--update-snapshots"));
        } else {
            assertTrue(warnings.isEmpty());
        }
    }

    @Test
    void updatesAreNotForcedByDefault() throws ParseException {
        assertTrue(
                CommonsCliMavenOptions.parse("test", new String[0]).updateAll().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLI", "maven.config", "atFile"})
    void layeredOptionsPreserveDeprecationWarnings(String source) throws ParseException {
        MavenOptions options = LayeredMavenOptions.layerMavenOptions(List.of(
                CommonsCliMavenOptions.parse("other", new String[] {"--update-all"}),
                CommonsCliMavenOptions.parse(source, new String[] {"--update-snapshots"})));
        List<String> warnings = new ArrayList<>();
        options.warnAboutDeprecatedOptions(request, warnings::add);

        assertTrue(options.updateAll().orElseThrow());
        assertEquals("Detected deprecated option use in " + source, warnings.get(0));
        assertTrue(warnings.get(1).contains("Use -U,--update-all instead."));
        assertEquals(2, warnings.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-UA", "-UM"})
    void perTypeUpdateOptionsRemainIndependent(String arg) throws ParseException {
        CommonsCliMavenOptions options = CommonsCliMavenOptions.parse("test", new String[] {arg});
        assertTrue(options.updateAll().isEmpty());
        if ("-UA".equals(arg)) {
            assertEquals("always", options.artifactsUpdatePolicy().orElseThrow());
            assertTrue(options.metadataUpdatePolicy().isEmpty());
        } else {
            assertTrue(options.artifactsUpdatePolicy().isEmpty());
            assertEquals("always", options.metadataUpdatePolicy().orElseThrow());
        }
    }

    @Test
    void helpDocumentsPreferredOptionAndDeprecatedAlias() throws ParseException {
        List<String> lines = new ArrayList<>();
        CommonsCliMavenOptions.parse("test", new String[0]).displayHelp(request, lines::add);
        String help = String.join(" ", lines).replaceAll("\\s+", " ");

        assertTrue(help.contains("-U,--update-all"));
        assertTrue(help.contains("equivalent to -UA -UM"));
        assertTrue(help.contains("--update-snapshots"));
        assertTrue(help.contains("<deprecated> Alias for --update-all."));
        assertFalse(help.contains("-U,--update-snapshots"));
    }
}
