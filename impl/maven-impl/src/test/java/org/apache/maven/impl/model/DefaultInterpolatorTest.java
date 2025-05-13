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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultInterpolatorTest {

    @Test
    void basicSubstitution() {
        Map<String, String> props = new HashMap<>();
        props.put("key0", "value0");
        props.put("key1", "${value1}");
        props.put("key2", "${value2}");

        performSubstitution(props, Map.of("value1", "sub_value1")::get);

        assertEquals("value0", props.get("key0"));
        assertEquals("sub_value1", props.get("key1"));
        assertEquals("", props.get("key2"));
    }

    @Test
    void basicSubstitutionWithContext() {
        HashMap<String, String> props = new HashMap<>();
        props.put("key0", "value0");
        props.put("key1", "${value1}");

        performSubstitution(props, Map.of("value1", "sub_value1")::get);

        assertEquals("value0", props.get("key0"));
        assertEquals("sub_value1", props.get("key1"));
    }

    @Test
    void substitutionFailures() {
        assertEquals("a}", substVars("a}", "b"));
        assertEquals("${a", substVars("${a", "b"));
    }

    @Test
    void emptyVariable() {
        assertEquals("", substVars("${}", "b"));
    }

    @Test
    void innerSubst() {
        assertEquals("c", substVars("${${a}}", "z", Map.of("a", "b", "b", "c")));
    }

    @Test
    void substLoop() {
        assertThrows(
                InterpolatorException.class,
                () -> substVars("${a}", "a"),
                "Expected substVars() to throw an InterpolatorException, but it didn't");
    }

    @Test
    void loopEmpty() {
        assertEquals("${a}", substVars("${a}", null, null, null, false));
    }

    @Test
    void loopEmpty2() {
        assertEquals("${a}", substVars("${a}", null, null, null, false));
    }

    @Test
    void substitutionEscape() {
        assertEquals("${a}", substVars("$\\{a${#}\\}", "b"));
        assertEquals("${a}", substVars("$\\{a\\}${#}", "b"));
        assertEquals("${a}", substVars("$\\{a\\}", "b"));
        assertEquals("\\\\", substVars("\\\\", "b"));
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

        assertEquals(map1, map2);
    }

    @Test
    void multipleEscapes() {
        LinkedHashMap<String, String> map1 = new LinkedHashMap<>();
        map1.put("a", "$\\\\{var}");
        map1.put("abc", "${ab}c");
        map1.put("ab", "${a}b");
        performSubstitution(map1);

        assertEquals("$\\{var}", map1.get("a"));
        assertEquals("$\\{var}b", map1.get("ab"));
        assertEquals("$\\{var}bc", map1.get("abc"));
    }

    @Test
    void preserveUnresolved() {
        Map<String, String> props = new HashMap<>();
        props.put("a", "${b}");
        assertEquals("", substVars("${b}", "a", props, null, true));
        assertEquals("${b}", substVars("${b}", "a", props, null, false));

        props.put("b", "c");
        assertEquals("c", substVars("${b}", "a", props, null, true));
        assertEquals("c", substVars("${b}", "a", props, null, false));

        props.put("c", "${d}${d}");
        assertEquals("${d}${d}", substVars("${d}${d}", "c", props, null, false));
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

        assertEquals("foo", props.get("a_cm"));
        assertEquals("bar", props.get("b_cm"));
        assertEquals("bar", props.get("c_cm"));

        assertEquals("bar", props.get("a_cp"));
        assertEquals("", props.get("b_cp"));
        assertEquals("", props.get("c_cp"));
    }

    @Test
    void ternary() {
        Map<String, String> props;

        props = new LinkedHashMap<>();
        props.put("foo", "-FOO");
        props.put("bar", "-BAR");
        props.put("version", "1.0${release:+${foo}:-${bar}}");
        performSubstitution(props);
        assertEquals("1.0-BAR", props.get("version"));

        props = new LinkedHashMap<>();
        props.put("release", "true");
        props.put("foo", "-FOO");
        props.put("bar", "-BAR");
        props.put("version", "1.0${release:+${foo}:-${bar}}");
        performSubstitution(props);
        assertEquals("1.0-FOO", props.get("version"));

        props = new LinkedHashMap<>();
        props.put("foo", "");
        props.put("bar", "-BAR");
        props.put("version", "1.0${release:+${foo}:-${bar}}");
        performSubstitution(props);
        assertEquals("1.0-BAR", props.get("version"));

        props = new LinkedHashMap<>();
        props.put("release", "true");
        props.put("foo", "");
        props.put("bar", "-BAR");
        props.put("version", "1.0${release:+${foo}:-${bar}}");
        performSubstitution(props);
        assertEquals("1.0", props.get("version"));

        props = new LinkedHashMap<>();
        props.put("version", "1.0${release:+:--BAR}");
        performSubstitution(props);
        assertEquals("1.0-BAR", props.get("version"));

        props = new LinkedHashMap<>();
        props.put("release", "true");
        props.put("version", "1.0${release:+:--BAR}");
        performSubstitution(props);
        assertEquals("1.0", props.get("version"));
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
        assertEquals("/Users/gnodet/.m2", props.get("maven.user.config"));

        props = new LinkedHashMap<>();
        props.put("user.home", "/Users/gnodet");
        props.put(
                "maven.user.config",
                "${env.MAVEN_XDG:+${env.XDG_CONFIG_HOME:-${user.home}/.config/maven}:-${user.home}/.m2}");
        props.put("env.MAVEN_XDG", "true");
        performSubstitution(props);
        assertEquals("/Users/gnodet/.config/maven", props.get("maven.user.config"));

        props = new LinkedHashMap<>();
        props.put("user.home", "/Users/gnodet");
        props.put(
                "maven.user.config",
                "${env.MAVEN_XDG:+${env.XDG_CONFIG_HOME:-${user.home}/.config/maven}:-${user.home}/.m2}");
        props.put("env.MAVEN_XDG", "true");
        props.put("env.XDG_CONFIG_HOME", "/Users/gnodet/.xdg/maven");
        performSubstitution(props);
        assertEquals("/Users/gnodet/.xdg/maven", props.get("maven.user.config"));
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
