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
package org.apache.maven.internal.impl.model.profile;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.internal.impl.DefaultModelVersionParser;
import org.apache.maven.internal.impl.DefaultVersionParser;
import org.apache.maven.internal.impl.model.DefaultInterpolator;
import org.apache.maven.internal.impl.model.DefaultPathTranslator;
import org.apache.maven.internal.impl.model.DefaultProfileActivationContext;
import org.apache.maven.internal.impl.model.ProfileActivationFilePathInterpolator;
import org.apache.maven.internal.impl.model.profile.ConditionParser.ExpressionFunction;
import org.apache.maven.internal.impl.model.rootlocator.DefaultRootLocator;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionParserTest {
    ConditionParser parser;
    Map<String, ExpressionFunction> functions;
    Function<String, String> propertyResolver;

    @BeforeEach
    void setUp() {
        ProfileActivationContext context = createMockContext();
        DefaultVersionParser versionParser =
                new DefaultVersionParser(new DefaultModelVersionParser(new GenericVersionScheme()));
        ProfileActivationFilePathInterpolator interpolator = new ProfileActivationFilePathInterpolator(
                new DefaultPathTranslator(),
                new AbstractProfileActivatorTest.FakeRootLocator(),
                new DefaultInterpolator());
        DefaultRootLocator rootLocator = new DefaultRootLocator();

        functions = ConditionProfileActivator.registerFunctions(context, versionParser, interpolator);
        propertyResolver = s -> ConditionProfileActivator.property(context, rootLocator, s);
        parser = new ConditionParser(functions, propertyResolver);
    }

    private ProfileActivationContext createMockContext() {
        DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        Properties systemProperties = new Properties();
        systemProperties.setProperty("os.name", "windows");
        systemProperties.setProperty("os.arch", "amd64");
        systemProperties.setProperty("os.version", "10.0");
        systemProperties.setProperty("java.version", "1.8.0_292");
        context.setSystemProperties(systemProperties);
        context.setModel(Model.newInstance());
        return context;
    }

    @Test
    void testStringLiterals() {
        assertEquals("Hello, World!", parser.parse("'Hello, World!'"));
        assertEquals("Hello, World!", parser.parse("\"Hello, World!\""));
    }

    @Test
    void testStringConcatenation() {
        assertEquals("HelloWorld", parser.parse("'Hello' + 'World'"));
        assertEquals("Hello123", parser.parse("'Hello' + 123"));
    }

    @Test
    void testLengthFunction() {
        assertEquals(13, parser.parse("length('Hello, World!')"));
        assertEquals(5, parser.parse("length(\"Hello\")"));
    }

    @Test
    void testCaseConversionFunctions() {
        assertEquals("HELLO", parser.parse("upper('hello')"));
        assertEquals("world", parser.parse("lower('WORLD')"));
    }

    @Test
    void testConcatFunction() {
        assertEquals("HelloWorld", parser.parse("'Hello' + 'World'"));
        assertEquals("The answer is 42", parser.parse("'The answer is ' + 42"));
        assertEquals("The answer is 42", parser.parse("'The answer is ' + 42.0"));
        assertEquals("The answer is 42", parser.parse("'The answer is ' + 42.0f"));
        assertEquals("Pi is approximately 3.14", parser.parse("'Pi is approximately ' + 3.14"));
        assertEquals("Pi is approximately 3.14", parser.parse("'Pi is approximately ' + 3.14f"));
    }

    @Test
    void testToString() {
        assertEquals("42", ConditionParser.toString(42));
        assertEquals("42", ConditionParser.toString(42L));
        assertEquals("42", ConditionParser.toString(42.0));
        assertEquals("42", ConditionParser.toString(42.0f));
        assertEquals("3.14", ConditionParser.toString(3.14));
        assertEquals("3.14", ConditionParser.toString(3.14f));
        assertEquals("true", ConditionParser.toString(true));
        assertEquals("false", ConditionParser.toString(false));
        assertEquals("hello", ConditionParser.toString("hello"));
    }

    @Test
    void testSubstringFunction() {
        assertEquals("World", parser.parse("substring('Hello, World!', 7, 12)"));
        assertEquals("World!", parser.parse("substring('Hello, World!', 7)"));
    }

    @Test
    void testIndexOf() {
        assertEquals(7, parser.parse("indexOf('Hello, World!', 'World')"));
        assertEquals(-1, parser.parse("indexOf('Hello, World!', 'OpenAI')"));
    }

    @Test
    void testInRange() {
        assertTrue((Boolean) parser.parse("inrange('1.8.0_292', '[1.8,2.0)')"));
        assertFalse((Boolean) parser.parse("inrange('1.7.0', '[1.8,2.0)')"));
    }

    @Test
    void testIfFunction() {
        assertEquals("long", parser.parse("if(length('test') > 3, 'long', 'short')"));
        assertEquals("short", parser.parse("if(length('hi') > 3, 'long', 'short')"));
    }

    @Test
    void testContainsFunction() {
        assertTrue((Boolean) parser.parse("contains('Hello, World!', 'World')"));
        assertFalse((Boolean) parser.parse("contains('Hello, World!', 'OpenAI')"));
    }

    @Test
    void testMatchesFunction() {
        assertTrue((Boolean) parser.parse("matches('test123', '\\w+')"));
        assertFalse((Boolean) parser.parse("matches('test123', '\\d+')"));
    }

    @Test
    void testComplexExpression() {
        String expression = "if(contains(lower('HELLO WORLD'), 'hello'), upper('success') + '!', 'failure')";
        assertEquals("SUCCESS!", parser.parse(expression));
    }

    @Test
    void testStringComparison() {
        assertTrue((Boolean) parser.parse("'abc' != 'cdf'"));
        assertFalse((Boolean) parser.parse("'abc' != 'abc'"));
        assertTrue((Boolean) parser.parse("'abc' == 'abc'"));
        assertFalse((Boolean) parser.parse("'abc' == 'cdf'"));
    }

    @Test
    void testParenthesesMismatch() {
        functions.put("property", args -> "foo");
        functions.put("inrange", args -> false);
        assertThrows(
                RuntimeException.class,
                () -> parser.parse(
                        "property('os.name') == 'windows' && property('os.arch') != 'amd64') && inrange(property('os.version'), '[99,)')"),
                "Should throw RuntimeException due to parentheses mismatch");

        assertThrows(
                RuntimeException.class,
                () -> parser.parse(
                        "(property('os.name') == 'windows' && property('os.arch') != 'amd64') && inrange(property('os.version'), '[99,)'"),
                "Should throw RuntimeException due to parentheses mismatch");

        // This should not throw an exception if parentheses are balanced and properties are handled correctly
        assertDoesNotThrow(
                () -> parser.parse(
                        "(property('os.name') == 'windows' && property('os.arch') != 'amd64') && inrange(property('os.version'), '[99,)')"));
    }

    @Test
    void testBasicArithmetic() {
        assertEquals(5.0, parser.parse("2 + 3"));
        assertEquals(10.0, parser.parse("15 - 5"));
        assertEquals(24.0, parser.parse("6 * 4"));
        assertEquals(3.0, parser.parse("9 / 3"));
    }

    @Test
    void testArithmeticPrecedence() {
        assertEquals(14.0, parser.parse("2 + 3 * 4"));
        assertEquals(20.0, parser.parse("(2 + 3) * 4"));
        assertEquals(11.0, parser.parse("15 - 6 + 2"));
        assertEquals(10.0, parser.parse("10 / 2 + 2 * 2.5"));
    }

    @Test
    void testFloatingPointArithmetic() {
        assertEquals(5.5, parser.parse("2.2 + 3.3"));
        assertEquals(0.1, (Double) parser.parse("3.3 - 3.2"), 1e-10);
        assertEquals(6.25, parser.parse("2.5 * 2.5"));
        assertEquals(2.5, parser.parse("5 / 2"));
    }

    @Test
    void testArithmeticComparisons() {
        assertTrue((Boolean) parser.parse("5 > 3"));
        assertTrue((Boolean) parser.parse("3 < 5"));
        assertTrue((Boolean) parser.parse("5 >= 5"));
        assertTrue((Boolean) parser.parse("3 <= 3"));
        assertTrue((Boolean) parser.parse("5 == 5"));
        assertTrue((Boolean) parser.parse("5 != 3"));
        assertFalse((Boolean) parser.parse("5 < 3"));
        assertFalse((Boolean) parser.parse("3 > 5"));
        assertFalse((Boolean) parser.parse("5 != 5"));
    }

    @Test
    void testComplexArithmeticExpressions() {
        assertFalse((Boolean) parser.parse("(2 + 3 * 4) > (10 + 5)"));
        assertTrue((Boolean) parser.parse("(2 + 3 * 4) < (10 + 5)"));
        assertTrue((Boolean) parser.parse("(10 / 2 + 3) == 8"));
        assertTrue((Boolean) parser.parse("(10 / 2 + 3) != 9"));
    }

    @Test
    void testArithmeticFunctions() {
        assertEquals(5.0, parser.parse("2 + 3"));
        assertEquals(2.0, parser.parse("5 - 3"));
        assertEquals(15.0, parser.parse("3 * 5"));
        assertEquals(2.5, parser.parse("5 / 2"));
    }

    @Test
    void testCombinedArithmeticAndLogic() {
        assertTrue((Boolean) parser.parse("(5 > 3) && (10 / 2 == 5)"));
        assertFalse((Boolean) parser.parse("(5 < 3) || (10 / 2 != 5)"));
        assertTrue((Boolean) parser.parse("2 + 3 == 1 * 5"));
    }

    @Test
    void testDivisionByZero() {
        assertThrows(ArithmeticException.class, () -> parser.parse("5 / 0"));
    }

    @Test
    void testPropertyAlias() {
        assertTrue((Boolean) parser.parse("${os.name} == 'windows'"));
        assertFalse((Boolean) parser.parse("${os.name} == 'linux'"));
        assertTrue((Boolean) parser.parse("${os.arch} == 'amd64' && ${os.name} == 'windows'"));
        assertThrows(RuntimeException.class, () -> parser.parse("${unclosed"));
    }

    @Test
    void testNestedPropertyAlias() {
        functions.put("property", args -> {
            if (args.get(0).equals("project.rootDirectory")) return "/home/user/project";
            return null;
        });
        functions.put("exists", args -> true); // Mock implementation

        Object result = parser.parse("exists('${project.rootDirectory}/someFile.txt')");
        assertTrue((Boolean) result);

        result = parser.parse("exists('${project.rootDirectory}/${nested.property}/someFile.txt')");
        assertTrue((Boolean) result);

        assertDoesNotThrow(() -> parser.parse("property('')"));
    }

    @Test
    void testToInt() {
        assertEquals(123, ConditionParser.toInt(123));
        assertEquals(123, ConditionParser.toInt(123L));
        assertEquals(123, ConditionParser.toInt(123.0));
        assertEquals(123, ConditionParser.toInt(123.5)); // This will truncate the decimal part
        assertEquals(123, ConditionParser.toInt("123"));
        assertEquals(123, ConditionParser.toInt("123.0"));
        assertEquals(123, ConditionParser.toInt("123.5")); // This will truncate the decimal part
        assertEquals(1, ConditionParser.toInt(true));
        assertEquals(0, ConditionParser.toInt(false));

        assertThrows(RuntimeException.class, () -> ConditionParser.toInt("not a number"));
        assertThrows(RuntimeException.class, () -> ConditionParser.toInt(new Object()));
    }
}
