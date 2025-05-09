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
package org.apache.maven.toolchain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
class RequirementMatcherFactoryTest {

    /**
     * Test of createExactMatcher method, of class RequirementMatcherFactory.
     */
    @Test
    void createExactMatcher() {
        RequirementMatcher matcher;
        matcher = RequirementMatcherFactory.createExactMatcher("foo");
        assertThat(matcher.matches("bar")).isFalse();
        assertThat(matcher.matches("foobar")).isFalse();
        assertThat(matcher.matches("foob")).isFalse();
        assertThat(matcher.matches("foo")).isTrue();
    }

    /**
     * Test of createVersionMatcher method, of class RequirementMatcherFactory.
     */
    @Test
    void createVersionMatcher() {
        RequirementMatcher matcher;
        matcher = RequirementMatcherFactory.createVersionMatcher("1.5.2");
        assertThat(matcher.matches("1.5")).isFalse();
        assertThat(matcher.matches("1.5.2")).isTrue();
        assertThat(matcher.matches("[1.4,1.5)")).isFalse();
        assertThat(matcher.matches("[1.5,1.5.2)")).isFalse();
        assertThat(matcher.matches("(1.5.2,1.6)")).isFalse();
        assertThat(matcher.matches("(1.4,1.5.2]")).isTrue();
        assertThat(matcher.matches("(1.5,)")).isTrue();
        assertThat(matcher.toString()).isEqualTo("1.5.2");

        // Ensure it is not printed as 1.5.0
        matcher = RequirementMatcherFactory.createVersionMatcher("1.5");
        assertThat(matcher.toString()).isEqualTo("1.5");
    }
}
