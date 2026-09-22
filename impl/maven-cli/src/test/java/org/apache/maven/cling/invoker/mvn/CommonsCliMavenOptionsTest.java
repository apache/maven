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
import java.util.Optional;

import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link CommonsCliMavenOptions} covering the {@code --skip-phases} and
 * {@code --skip-tests} options introduced in MNG-13230.
 */
class CommonsCliMavenOptionsTest {

    // -------------------------------------------------------------------------
    // --skip-phases
    // -------------------------------------------------------------------------

    @Test
    void skippedPhasesNotPresentByDefault() throws ParseException {
        CommonsCliMavenOptions options = CommonsCliMavenOptions.parse("test", new String[] {"verify"});
        assertEquals(Optional.empty(), options.skippedPhases());
    }

    @Test
    void skippedPhasesSingleValue() throws ParseException {
        // Use = form so Commons CLI does not consume the goal "verify" as a phase value.
        CommonsCliMavenOptions options =
                CommonsCliMavenOptions.parse("test", new String[] {"--skip-phases=test", "verify"});
        assertEquals(Optional.of(List.of("test")), options.skippedPhases());
    }

    @Test
    void skippedPhasesMultipleValuesCommaSeparated() throws ParseException {
        CommonsCliMavenOptions options =
                CommonsCliMavenOptions.parse("test", new String[] {"--skip-phases=test,integration-test", "verify"});
        assertEquals(Optional.of(List.of("test", "integration-test")), options.skippedPhases());
    }

    /**
     * Whitespace around comma-separated values must be stripped so that
     * {@code --skip-phases "test, integration-test"} works like
     * {@code --skip-phases "test,integration-test"}.
     * <p>
     * Commons CLI splits on the {@code valueSeparator(',')} and returns individual tokens;
     * the implementation must strip leading/trailing whitespace from each token.
     */
    @Test
    void skippedPhasesStripsWhitespace() throws ParseException {
        CommonsCliMavenOptions options =
                CommonsCliMavenOptions.parse("test", new String[] {"--skip-phases=test, integration-test", "verify"});
        assertEquals(Optional.of(List.of("test", "integration-test")), options.skippedPhases());
    }

    @Test
    void skippedPhasesShortOption() throws ParseException {
        // Short option with hasArgs(): -sp consumes the next token as its value.
        // No positional goal follows, so the value is unambiguously "test".
        CommonsCliMavenOptions options = CommonsCliMavenOptions.parse("test", new String[] {"-sp", "test"});
        assertEquals(Optional.of(List.of("test")), options.skippedPhases());
    }

    // -------------------------------------------------------------------------
    // --skip-tests
    // -------------------------------------------------------------------------

    @Test
    void skipTestsNotPresentByDefault() throws ParseException {
        CommonsCliMavenOptions options = CommonsCliMavenOptions.parse("test", new String[] {"verify"});
        assertEquals(Optional.empty(), options.skipTests());
    }

    @Test
    void skipTestsLongOption() throws ParseException {
        CommonsCliMavenOptions options = CommonsCliMavenOptions.parse("test", new String[] {"--skip-tests", "verify"});
        assertEquals(Optional.of(Boolean.TRUE), options.skipTests());
    }

    @Test
    void skipTestsShortOption() throws ParseException {
        CommonsCliMavenOptions options = CommonsCliMavenOptions.parse("test", new String[] {"-st", "verify"});
        assertEquals(Optional.of(Boolean.TRUE), options.skipTests());
    }

    // -------------------------------------------------------------------------
    // --skip-phases and --skip-tests can be combined
    // -------------------------------------------------------------------------

    @Test
    void skipPhasesAndSkipTestsCanCoexist() throws ParseException {
        CommonsCliMavenOptions options =
                CommonsCliMavenOptions.parse("test", new String[] {"--skip-phases=verify", "--skip-tests", "install"});
        assertEquals(Optional.of(List.of("verify")), options.skippedPhases());
        assertEquals(Optional.of(Boolean.TRUE), options.skipTests());
    }
}
