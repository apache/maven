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
package org.apache.maven.impl.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.maven.api.services.InterpolatorException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class DefaultInterpolatorTest {

    @Test
    void basicSubstitution() {
        Map<String, String> props = new HashMap<>();
        props.put("key0", "value0");
        props.put("key1", "${value1}");
        props.put("key2", "${value2}");

        performSubstitution(props, Map.of("value1", "sub_value1")::get);

        assertThat(props.get("key0")).isEqualTo("value0");
        assertThat(props.get("key1")).isEqualTo("sub_value1");
        assertThat(props.get("key2")).isEqualTo("");
    }

    @Test
    void basicSubstitutionWithContext() {
        HashMap<String, String> props = new HashMap<>();
        props.put("key0", "value0");
        props.put("key1", "${value1}");

        performSubstitution(props, Map.of("value1", "sub_value1")::get);

        assertThat(props.get("key0")).isEqualTo("value0");
        assertThat(props.get("key1")).isEqualTo("sub_value1");
    }

    @Test
    void substitutionFailures() {
        assertThat(substVars("a}", "b")).isEqualTo("a}");
        assertThat(substVars("${a", "b")).isEqualTo("${a");
    }

    @Test
    void emptyVariable() {
        assertThat(substVars("${}", "b")).isEqualTo("");
    }

    @Test
    void innerSubst() {
        assertThat(substVars("${${a}}", "z", Map.of("a", "b", "b", "c"))).isEqualTo("c");
    }

    @Test
    void substLoop() {
        assertThatExceptionOfType(InterpolatorException.class).as("Expected substVars() to throw an InterpolatorException, but it didn't").isThrownBy(() -> substVars("${a}", "a"));
    }

    @Test
    void loopEmpty() {
        assertThat(substVars("${a}", null, null, null, false)).isEqualTo("${a}");
    }

    @Test
    void loopEmpty2() {
        assertThat(substVars("${a}", null, null, null, false)).isEqualTo("${a}");
    }

    @Test
    void substitutionEscape() {
        assertThat(substVars("$\\{a${#}\\}", "b")).isEqualTo("${a}");
        assertThat(substVars("$\\{a\\}${#}", "b")).isEqualTo("${a}");
        assertThat(substVars("$\\{a\\}", "b")).isEqualTo("${a}");
        assertThat(substVars("\\\\", "b")).isEqualTo("\\\\");
    }

    @Test
    void substitutionOrder() {
        LinkedHashMap<String, String> map1 = new LinkedHashMap<>();
        map1.put("a", "$\\\\{var}");
        map1.put("abc", "${ab}c");
        map1.put("ab", "${a}b");
        performSubstitution(map1);

        LinkedHashMap<String, String> map2 = new LinkedHashMap<>();
        map2.put("a", "$\\\\{var}");
        map2.put("ab", "${a}b");
        map2.put("abc", "${ab}c");
        performSubstitution(map2);

        assertThat(map2).isEqualTo(map1);
    }

    @Test
    void multipleEscapes() {
        LinkedHashMap<String, String> map1 = new LinkedHashMap<>();
        map1.put("a", "$\\\\{var}");
        map1.put("abc", "${ab}c");
        map1.put("ab", "${a}b");
        performSubstitution(map1);

        assertThat(map1.get("a")).isEqualTo("$\\{var}");
        assertThat(map1.get("ab")).isEqualTo("$\\{var}b");
        assertThat(map1.get("abc")).isEqualTo("$\\{var}bc");
    }

    @Test
    void preserveUnresolved() {
        Map<String, String> props = new HashMap<>();
        props.put("a", "${b}");
        assertThat(substVars("${b}", "a", props, null, true)).isEqualTo("");
        assertThat(substVars("${b}", "a", props, null, false)).isEqualTo("${b}");

        props.put("b", "c");
        assertThat(substVars("${b}", "a", props, null, true)).isEqualTo("c");
        assertThat(substVars("${b}", "a", props, null, false)).isEqualTo("c");

        props.put("c", "${d}${d}");
        assertThat(substVars("${d}${d}", "c", props, null, false)).isEqualTo("${d}${d}");
    }

    @Test
    void expansion() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("a", "foo");
        props.put("b", "");

        props.put("a_cm", "${a:-bar}");
        props.put("b_cm", "${b:-bar}");
        props.put("c_cm", "${c:-bar}");

        props.put("a_cp", "${a:+bar}");
        props.put("b_cp", "${b:+bar}");
        props.put("c_cp", "${c:+bar}");

        performSubstitution(props);

        assertThat(props.get("a_cm")).isEqualTo("foo");
        assertThat(props.get("b_cm")).isEqualTo("bar");
        assertThat(props.get("c_cm")).isEqualTo("bar");

        assertThat(props.get("a_cp")).isEqualTo("bar");
        assertThat(props.get("b_cp")).isEqualTo("");
        assertThat(props.get("c_cp")).isEqualTo("");
    }

    @Test
    void ternary() {
        Map<String, String> props;

        props = new LinkedHashMap<>();
        props.put("foo", "-FOO");
        props.put("bar", "-BAR");
        props.put("version", "1.0${release:+${foo}:-${bar}}");
        performSubstitution(props);
        assertThat(props.get("version")).isEqualTo("1.0-BAR");

        props = new LinkedHashMap<>();
        props.put("release", "true");
        props.put("foo", "-FOO");
        props.put("bar", "-BAR");
        props.put("version", "1.0${release:+${foo}:-${bar}}");
        performSubstitution(props);
        assertThat(props.get("version")).isEqualTo("1.0-FOO");

        props = new LinkedHashMap<>();
        props.put("foo", "");
        props.put("bar", "-BAR");
        props.put("version", "1.0${release:+${foo}:-${bar}}");
        performSubstitution(props);
        assertThat(props.get("version")).isEqualTo("1.0-BAR");

        props = new LinkedHashMap<>();
        props.put("release", "true");
        props.put("foo", "");
        props.put("bar", "-BAR");
        props.put("version", "1.0${release:+${foo}:-${bar}}");
        performSubstitution(props);
        assertThat(props.get("version")).isEqualTo("1.0");

        props = new LinkedHashMap<>();
        props.put("version", "1.0${release:+:--BAR}");
        performSubstitution(props);
        assertThat(props.get("version")).isEqualTo("1.0-BAR");

        props = new LinkedHashMap<>();
        props.put("release", "true");
        props.put("version", "1.0${release:+:--BAR}");
        performSubstitution(props);
        assertThat(props.get("version")).isEqualTo("1.0");
    }

    @Test
    void xdg() {
        Map<String, String> props;

        props = new LinkedHashMap<>();
        props.put("user.home", "/Users/gnodet");
        props.put(
                "maven.user.config",
                "${env.MAVEN_XDG:+${env.XDG_CONFIG_HOME:-${user.home}/.config/maven}:-${user.home}/.m2}");
        performSubstitution(props);
        assertThat(props.get("maven.user.config")).isEqualTo("/Users/gnodet/.m2");

        props = new LinkedHashMap<>();
        props.put("user.home", "/Users/gnodet");
        props.put(
                "maven.user.config",
                "${env.MAVEN_XDG:+${env.XDG_CONFIG_HOME:-${user.home}/.config/maven}:-${user.home}/.m2}");
        props.put("env.MAVEN_XDG", "true");
        performSubstitution(props);
        assertThat(props.get("maven.user.config")).isEqualTo("/Users/gnodet/.config/maven");

        props = new LinkedHashMap<>();
        props.put("user.home", "/Users/gnodet");
        props.put(
                "maven.user.config",
                "${env.MAVEN_XDG:+${env.XDG_CONFIG_HOME:-${user.home}/.config/maven}:-${user.home}/.m2}");
        props.put("env.MAVEN_XDG", "true");
        props.put("env.XDG_CONFIG_HOME", "/Users/gnodet/.xdg/maven");
        performSubstitution(props);
        assertThat(props.get("maven.user.config")).isEqualTo("/Users/gnodet/.xdg/maven");
    }

    private void performSubstitution(Map<String, String> props) {
        performSubstitution(props, null);
    }

    private void performSubstitution(Map<String, String> props, UnaryOperator<String> callback) {
        new DefaultInterpolator().performSubstitution(props, callback);
    }

    private String substVars(
            String val,
            String currentKey,
            Map<String, String> configProps,
            UnaryOperator<String> callback,
            boolean defaultsToEmptyString) {
        return DefaultInterpolator.substVars(val, currentKey, null, configProps, callback, null, defaultsToEmptyString);
    }

    private String substVars(String val, String currentKey) {
        return DefaultInterpolator.substVars(val, currentKey, null, null, null, null, true);
    }

    private String substVars(String val, String currentKey, Map<String, String> configProps) {
        return new DefaultInterpolator().substVars(val, currentKey, null, configProps);
    }
}
