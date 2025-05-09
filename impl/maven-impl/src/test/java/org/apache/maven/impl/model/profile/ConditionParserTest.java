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
package org.apache.maven.impl.model.profile;

import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.impl.DefaultModelVersionParser;
import org.apache.maven.impl.DefaultVersionParser;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.apache.maven.impl.model.DefaultPathTranslator;
import org.apache.maven.impl.model.DefaultProfileActivationContext;
import org.apache.maven.impl.model.profile.ConditionParser.ExpressionFunction;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ConditionParserTest {
    ConditionParser parser;
    Map<String, ExpressionFunction> functions;
    UnaryOperator<String> propertyResolver;

    @BeforeEach
    void setUp() {
        ProfileActivationContext context = createMockContext();
        DefaultVersionParser versionParser =
                new DefaultVersionParser(new DefaultModelVersionParser(new GenericVersionScheme()));

        ConditionProfileActivator activator = new ConditionProfileActivator(versionParser, new DefaultInterpolator());
        functions = activator.registerFunctions(context, versionParser);
        propertyResolver = s -> activator.property(context, s);
        parser = new ConditionParser(functions, propertyResolver);
    }

    private ProfileActivationContext createMockContext() {
        DefaultProfileActivationContext context = new DefaultProfileActivationContext(
                new DefaultPathTranslator(),
                new AbstractProfileActivatorTest.FakeRootLocator(),
                new DefaultInterpolator());
        context.setSystemProperties(Map.of(
                "os.name", "windows",
                "os.arch", "amd64",
                "os.version", "10.0",
                "java.version", "1.8.0_292"));
        context.setModel(Model.newInstance());
        return context;
    }

    @Test
    void stringLiterals() {
        assertThat(parser.parse("'Hello, World!'")).isEqualTo("Hello, World!");
        assertThat(parser.parse("\"Hello, World!\"")).isEqualTo("Hello, World!");
    }

    @Test
    void stringConcatenation() {
        assertThat(parser.parse("'Hello' + 'World'")).isEqualTo("HelloWorld");
        assertThat(parser.parse("'Hello' + 123")).isEqualTo("Hello123");
    }

    @Test
    void lengthFunction() {
        assertThat(parser.parse("length('Hello, World!')")).isEqualTo(13);
        assertThat(parser.parse("length(\"Hello\")")).isEqualTo(5);
    }

    @Test
    void caseConversionFunctions() {
        assertThat(parser.parse("upper('hello')")).isEqualTo("HELLO");
        assertThat(parser.parse("lower('WORLD')")).isEqualTo("world");
    }

    @Test
    void concatFunction() {
        assertThat(parser.parse("'Hello' + 'World'")).isEqualTo("HelloWorld");
        assertThat(parser.parse("'The answer is ' + 42")).isEqualTo("The answer is 42");
        assertThat(parser.parse("'The answer is ' + 42.0")).isEqualTo("The answer is 42");
        assertThat(parser.parse("'The answer is ' + 42.0f")).isEqualTo("The answer is 42");
        assertThat(parser.parse("'Pi is approximately ' + 3.14")).isEqualTo("Pi is approximately 3.14");
        assertThat(parser.parse("'Pi is approximately ' + 3.14f")).isEqualTo("Pi is approximately 3.14");
    }

    @Test
    void testToString() {
        assertThat(ConditionParser.toString(42)).isEqualTo("42");
        assertThat(ConditionParser.toString(42L)).isEqualTo("42");
        assertThat(ConditionParser.toString(42.0)).isEqualTo("42");
        assertThat(ConditionParser.toString(42.0f)).isEqualTo("42");
        assertThat(ConditionParser.toString(3.14)).isEqualTo("3.14");
        assertThat(ConditionParser.toString(3.14f)).isEqualTo("3.14");
        assertThat(ConditionParser.toString(true)).isEqualTo("true");
        assertThat(ConditionParser.toString(false)).isEqualTo("false");
        assertThat(ConditionParser.toString("hello")).isEqualTo("hello");
    }

    @Test
    void substringFunction() {
        assertThat(parser.parse("substring('Hello, World!', 7, 12)")).isEqualTo("World");
        assertThat(parser.parse("substring('Hello, World!', 7)")).isEqualTo("World!");
    }

    @Test
    void indexOf() {
        assertThat(parser.parse("indexOf('Hello, World!', 'World')")).isEqualTo(7);
        assertThat(parser.parse("indexOf('Hello, World!', 'OpenAI')")).isEqualTo(-1);
    }

    @Test
    void inRange() {
        assertThat((Boolean) parser.parse("inrange('1.8.0_292', '[1.8,2.0)')")).isTrue();
        assertThat((Boolean) parser.parse("inrange('1.7.0', '[1.8,2.0)')")).isFalse();
    }

    @Test
    void ifFunction() {
        assertThat(parser.parse("if(length('test') > 3, 'long', 'short')")).isEqualTo("long");
        assertThat(parser.parse("if(length('hi') > 3, 'long', 'short')")).isEqualTo("short");
    }

    @Test
    void containsFunction() {
        assertThat((Boolean) parser.parse("contains('Hello, World!', 'World')")).isTrue();
        assertThat((Boolean) parser.parse("contains('Hello, World!', 'OpenAI')"))
                .isFalse();
    }

    @Test
    void matchesFunction() {
        assertThat((Boolean) parser.parse("matches('test123', '\\w+')")).isTrue();
        assertThat((Boolean) parser.parse("matches('test123', '\\d+')")).isFalse();
    }

    @Test
    void complexExpression() {
        String expression = "if(contains(lower('HELLO WORLD'), 'hello'), upper('success') + '!', 'failure')";
        assertThat(parser.parse(expression)).isEqualTo("SUCCESS!");
    }

    @Test
    void stringComparison() {
        assertThat((Boolean) parser.parse("'abc' != 'cdf'")).isTrue();
        assertThat((Boolean) parser.parse("'abc' != 'abc'")).isFalse();
        assertThat((Boolean) parser.parse("'abc' == 'abc'")).isTrue();
        assertThat((Boolean) parser.parse("'abc' == 'cdf'")).isFalse();
    }

    @Test
    void parenthesesMismatch() {
        functions.put("property", args -> "foo");
        functions.put("inrange", args -> false);
        assertThatExceptionOfType(RuntimeException.class)
                .as("Should throw RuntimeException due to parentheses mismatch")
                .isThrownBy(
                        () -> parser.parse(
                                "property('os.name') == 'windows' && property('os.arch') != 'amd64') && inrange(property('os.version'), '[99,)')"));

        assertThatExceptionOfType(RuntimeException.class)
                .as("Should throw RuntimeException due to parentheses mismatch")
                .isThrownBy(
                        () -> parser.parse(
                                "(property('os.name') == 'windows' && property('os.arch') != 'amd64') && inrange(property('os.version'), '[99,)'"));

        // This should not throw an exception if parentheses are balanced and properties are handled correctly
        assertDoesNotThrow(
                () -> parser.parse(
                        "(property('os.name') == 'windows' && property('os.arch') != 'amd64') && inrange(property('os.version'), '[99,)')"));
    }

    @Test
    void basicArithmetic() {
        assertThat(parser.parse("2 + 3")).isEqualTo(5.0);
        assertThat(parser.parse("15 - 5")).isEqualTo(10.0);
        assertThat(parser.parse("6 * 4")).isEqualTo(24.0);
        assertThat(parser.parse("9 / 3")).isEqualTo(3.0);
    }

    @Test
    void arithmeticPrecedence() {
        assertThat(parser.parse("2 + 3 * 4")).isEqualTo(14.0);
        assertThat(parser.parse("(2 + 3) * 4")).isEqualTo(20.0);
        assertThat(parser.parse("15 - 6 + 2")).isEqualTo(11.0);
        assertThat(parser.parse("10 / 2 + 2 * 2.5")).isEqualTo(10.0);
    }

    @Test
    void floatingPointArithmetic() {
        assertThat(parser.parse("2.2 + 3.3")).isEqualTo(5.5);
        assertThat((Double) parser.parse("3.3 - 3.2")).isCloseTo(0.1, within(1e-10));
        assertThat(parser.parse("2.5 * 2.5")).isEqualTo(6.25);
        assertThat(parser.parse("5 / 2")).isEqualTo(2.5);
    }

    @Test
    void arithmeticComparisons() {
        assertThat((Boolean) parser.parse("5 > 3")).isTrue();
        assertThat((Boolean) parser.parse("3 < 5")).isTrue();
        assertThat((Boolean) parser.parse("5 >= 5")).isTrue();
        assertThat((Boolean) parser.parse("3 <= 3")).isTrue();
        assertThat((Boolean) parser.parse("5 == 5")).isTrue();
        assertThat((Boolean) parser.parse("5 != 3")).isTrue();
        assertThat((Boolean) parser.parse("5 < 3")).isFalse();
        assertThat((Boolean) parser.parse("3 > 5")).isFalse();
        assertThat((Boolean) parser.parse("5 != 5")).isFalse();
    }

    @Test
    void complexArithmeticExpressions() {
        assertThat((Boolean) parser.parse("(2 + 3 * 4) > (10 + 5)")).isFalse();
        assertThat((Boolean) parser.parse("(2 + 3 * 4) < (10 + 5)")).isTrue();
        assertThat((Boolean) parser.parse("(10 / 2 + 3) == 8")).isTrue();
        assertThat((Boolean) parser.parse("(10 / 2 + 3) != 9")).isTrue();
    }

    @Test
    void arithmeticFunctions() {
        assertThat(parser.parse("2 + 3")).isEqualTo(5.0);
        assertThat(parser.parse("5 - 3")).isEqualTo(2.0);
        assertThat(parser.parse("3 * 5")).isEqualTo(15.0);
        assertThat(parser.parse("5 / 2")).isEqualTo(2.5);
    }

    @Test
    void combinedArithmeticAndLogic() {
        assertThat((Boolean) parser.parse("(5 > 3) && (10 / 2 == 5)")).isTrue();
        assertThat((Boolean) parser.parse("(5 < 3) || (10 / 2 != 5)")).isFalse();
        assertThat((Boolean) parser.parse("2 + 3 == 1 * 5")).isTrue();
    }

    @Test
    void divisionByZero() {
        assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> parser.parse("5 / 0"));
    }

    @Test
    void propertyAlias() {
        assertThat((Boolean) parser.parse("${os.name} == 'windows'")).isTrue();
        assertThat((Boolean) parser.parse("${os.name} == 'linux'")).isFalse();
        assertThat((Boolean) parser.parse("${os.arch} == 'amd64' && ${os.name} == 'windows'"))
                .isTrue();
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> parser.parse("${unclosed"));
    }

    @Test
    void nestedPropertyAlias() {
        functions.put("property", args -> {
            if (args.get(0).equals("project.rootDirectory")) {
                return "/home/user/project";
            }
            return null;
        });
        functions.put("exists", args -> true); // Mock implementation

        Object result = parser.parse("exists('${project.rootDirectory}/someFile.txt')");
        assertThat((Boolean) result).isTrue();

        result = parser.parse("exists('${project.rootDirectory}/${nested.property}/someFile.txt')");
        assertThat((Boolean) result).isTrue();

        assertDoesNotThrow(() -> parser.parse("property('')"));
    }

    @Test
    void toInt() {
        assertThat(ConditionParser.toInt(123)).isEqualTo(123);
        assertThat(ConditionParser.toInt(123L)).isEqualTo(123);
        assertThat(ConditionParser.toInt(123.0)).isEqualTo(123);
        assertThat(ConditionParser.toInt(123.5)).isEqualTo(123); // This will truncate the decimal part
        assertThat(ConditionParser.toInt("123")).isEqualTo(123);
        assertThat(ConditionParser.toInt("123.0")).isEqualTo(123);
        assertThat(ConditionParser.toInt("123.5")).isEqualTo(123); // This will truncate the decimal part
        assertThat(ConditionParser.toInt(true)).isEqualTo(1);
        assertThat(ConditionParser.toInt(false)).isEqualTo(0);

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> ConditionParser.toInt("not a number"));
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> ConditionParser.toInt(new Object()));
    }
}
