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

import java.util.List;

import org.apache.maven.api.reactor.Alias;
import org.apache.maven.api.reactor.ReactorConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MavenParserAliasTest {

    // --- helpers ---

    private static Alias alias(String name, String... expansion) {
        return Alias.newBuilder().name(name).args(List.of(expansion)).build();
    }

    private static ReactorConfig config(Alias... aliases) {
        return ReactorConfig.newBuilder().aliases(List.of(aliases)).build();
    }

    // --- null / empty guards ---

    @Test
    void nullReactorConfigReturnsOriginal() {
        List<String> args = List.of("clean", "install");
        assertSame(args, MavenParser.expandAlias(args, null));
    }

    @Test
    void emptyAliasesReturnsOriginal() {
        List<String> args = List.of("clean", "install");
        assertSame(
                args, MavenParser.expandAlias(args, ReactorConfig.newBuilder().build()));
    }

    // --- no match ---

    @Test
    void noMatchReturnsOriginal() {
        List<String> args = List.of("-B", "clean", "install");
        ReactorConfig rc = config(alias("ci", "clean", "install", "-Prelease"));
        assertSame(args, MavenParser.expandAlias(args, rc));
    }

    // --- single expansion ---

    @Test
    void singleAliasExpanded() {
        ReactorConfig rc = config(alias("ci", "clean", "install", "-Prelease"));
        assertEquals(List.of("-B", "clean", "install", "-Prelease"), MavenParser.expandAlias(List.of("-B", "ci"), rc));
    }

    // --- multiple expansions in one invocation ---

    @Test
    void multipleAliasesAllExpanded() {
        ReactorConfig rc = config(alias("ci", "clean", "install"), alias("rel", "-Prelease", "deploy"));
        assertEquals(
                List.of("clean", "install", "-Prelease", "deploy"), MavenParser.expandAlias(List.of("ci", "rel"), rc));
    }

    @Test
    void aliasAmongFlagsExpandedInPlace() {
        ReactorConfig rc = config(alias("ci", "clean", "install"));
        assertEquals(
                List.of("-B", "clean", "install", "-Prelease"),
                MavenParser.expandAlias(List.of("-B", "ci", "-Prelease"), rc));
    }

    @Test
    void flagAliasExpanded() {
        // An alias named "--skip-tests" can be defined and will be expanded
        ReactorConfig rc =
                config(alias("--skip-tests", "-Dmaven.lifecycle.filter=phase(test),phase(integration-test)"));
        assertEquals(
                List.of("-B", "-Dmaven.lifecycle.filter=phase(test),phase(integration-test)"),
                MavenParser.expandAlias(List.of("-B", "--skip-tests"), rc));
    }

    // --- non-recursive ---

    @Test
    void expansionIsNotRecursive() {
        // "ci" expands to ["fast"], "fast" is also an alias — but should NOT be re-expanded
        ReactorConfig rc = config(alias("ci", "fast"), alias("fast", "clean", "install"));
        assertEquals(List.of("fast"), MavenParser.expandAlias(List.of("ci"), rc));
    }

    // --- lazy allocation: original list returned when no expansion happens ---

    @Test
    void noAliasMatchSameListInstance() {
        ReactorConfig rc = config(alias("ci", "clean", "install"));
        List<String> args = List.of("-B", "-q");
        assertSame(args, MavenParser.expandAlias(args, rc));
    }

    // --- alias name validation ---

    @Test
    void blankAliasNameThrows() {
        ReactorConfig rc = config(alias("", "clean"));
        assertThrows(
                IllegalArgumentException.class,
                () -> MavenParser.expandAlias(List.of("clean"), rc),
                "Blank alias name must throw");
    }
}
